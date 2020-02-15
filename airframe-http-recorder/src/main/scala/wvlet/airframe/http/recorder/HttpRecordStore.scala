/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.http.recorder

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Base64, Locale}

import com.twitter.finagle.http.{Request, Response}
import com.twitter.io.Buf
import wvlet.airframe.jdbc.{DbConfig, SQLiteConnectionPool}
import wvlet.airframe.metrics.TimeWindow
import wvlet.log.LogSupport

/**
  * Recorder for HTTP server responses
  */
class HttpRecordStore(val recorderConfig: HttpRecorderConfig, dropSession: Boolean = false, inMemory: Boolean = false)
    extends AutoCloseable
    with LogSupport {
  private val requestCounter = scala.collection.mutable.Map.empty[Int, AtomicInteger]
  private val connectionPool = {
    val dbFilePath = if (inMemory) ":memory:" else recorderConfig.sqliteFilePath
    new SQLiteConnectionPool(DbConfig.ofSQLite(dbFilePath))
  }
  private def recordTableName = recorderConfig.recordTableName

  // Increment this value if we need to change the table format.
  private val recordFormatVersion = 4

  init

  private def createRecorderInfoTable: Unit = {
    connectionPool.executeUpdate("create table if not exists recorder_info(format_version integer primary key)")
  }

  protected def init {
    // Support recorder version migration for persistent records
    createRecorderInfoTable
    val lastVersion: Option[Int] = connectionPool.executeQuery("select format_version from recorder_info limit 1") {
      handler =>
        if (handler.next()) {
          Some(handler.getInt(1))
        } else {
          None
        }
    }

    def setVersion: Unit = {
      clearAllRecords
      // Record the current record format version
      connectionPool.executeUpdate(
        s"insert into recorder_info(format_version) values(${recordFormatVersion}) ON CONFLICT(format_version) DO UPDATE SET format_version=${recordFormatVersion}"
      )
    }

    lastVersion match {
      case None => setVersion
      case Some(last) if last != recordFormatVersion =>
        warn(s"Record format version has been changed from ${last} to ${recordFormatVersion}")
        connectionPool.executeUpdate("drop table if exists recorder_info") // Support schema migration
        createRecorderInfoTable
        setVersion
      case _ => // do nothing
    }

    // Prepare a database table for recording HttpRecord
    connectionPool.executeUpdate(HttpRecord.createTableSQL(recordTableName))
    connectionPool.executeUpdate(
      s"create index if not exists ${recordTableName}_index on ${recordTableName} (session, requestHash)"
    )
    // TODO: Detect schema change
    if (dropSession) {
      clearSession
    }
    cleanupExpiredRecords
  }

  private def clearAllRecords: Unit = {
    if (!inMemory) {
      warn(s"Deleting all records in ${recorderConfig.sqliteFilePath}")
      connectionPool.executeUpdate(s"drop table if exists ${recordTableName}")
    }
  }

  def clearSession: Unit = {
    if (!inMemory) {
      warn(s"Deleting old session records for session:${recorderConfig.sessionName}")
      connectionPool.executeUpdate(s"delete from ${recordTableName} where session = '${recorderConfig.sessionName}'")
    }
  }

  private def cleanupExpiredRecords: Unit = {
    recorderConfig.expirationTime match {
      case None =>
      // Do nothing
      case Some(expire) =>
        val duration = TimeWindow.withUTC.parse(expire)
        val diffSec  = duration.endUnixTime - duration.startUnixTime

        val deletedRows = connectionPool.executeUpdate(
          s"delete from ${recordTableName} where session = '${recorderConfig.sessionName}' and strftime('%s', 'now') - strftime('%s', createdAt) >= ${diffSec}"
        )

        if (deletedRows > 0) {
          warn(
            s"Deleted ${deletedRows} expired records from session:${recorderConfig.sessionName}, db:${recorderConfig.sqliteFilePath}"
          )
        }
    }
  }

  def resetCounter: Unit = {
    requestCounter.clear()
  }

  def numRecordsInSession: Long = {
    connectionPool.executeQuery(
      s"select count(1) cnt from ${recordTableName} where session = '${recorderConfig.sessionName}'"
    ) { rs =>
      if (rs.next()) {
        rs.getLong(1)
      } else {
        0L
      }
    }
  }

  def findNext(request: Request, incrementHitCount: Boolean = true): Option[HttpRecord] = {
    val rh = recorderConfig.requestMatcher.computeHash(request)

    // If there are multiple records for the same request, use the counter to find
    // n-th request, where n is the access count to the same path
    val counter  = requestCounter.getOrElseUpdate(rh, new AtomicInteger())
    val hitCount = if (incrementHitCount) counter.getAndIncrement() else counter.get()
    trace(s"findNext: request hash: ${rh} for ${request}, hitCount: ${hitCount}")
    connectionPool.queryWith(
      // Get the next request matching the requestHash
      s"select * from ${recordTableName} where session = ? and requestHash = ? order by createdAt limit 1 offset ?"
    ) { prepare =>
      prepare.setString(1, recorderConfig.sessionName)
      prepare.setInt(2, rh)
      prepare.setInt(3, hitCount)
    } { rs => HttpRecord.read(rs).headOption }
  }

  def record(request: Request, response: Response): Unit = {
    val rh = recorderConfig.requestMatcher.computeHash(request)

    val httpHeadersForRecording: Seq[(String, String)] =
      request.headerMap.toSeq.filterNot { x => recorderConfig.excludeHeaderFilterForRecording(x._1, x._2) }
    val entry = HttpRecord(
      recorderConfig.sessionName,
      requestHash = rh,
      method = request.method.toString(),
      destHost = recorderConfig.destAddress.hostAndPort,
      path = request.uri,
      requestHeader = httpHeadersForRecording,
      requestBody = HttpRecordStore.encodeToBase64(request.content),
      responseCode = response.statusCode,
      responseHeader = response.headerMap.toSeq,
      responseBody = HttpRecordStore.encodeToBase64(response.content),
      createdAt = Instant.now()
    )

    trace(s"record: request hash ${rh} for ${request} -> ${entry.summary}")
    connectionPool.withConnection { conn => entry.insertInto(recordTableName, conn) }
  }

  override def close(): Unit = {
    connectionPool.stop
  }
}

object HttpRecordStore {
  def encodeToBase64(content: Buf): String = {
    val buf = new Array[Byte](content.length)
    content.write(buf, 0)

    val encoder = Base64.getEncoder
    encoder.encodeToString(buf)
  }

  def decodeFromBase64(base64String: String): Array[Byte] = {
    val decoder = Base64.getDecoder
    decoder.decode(base64String)
  }
}
