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
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

import com.twitter.finagle.http.{Request, Response}
import wvlet.airframe.jdbc.{DbConfig, SQLiteConnectionPool}
import wvlet.log.LogSupport

/**
  * Recorder for HTTP server responses
  */
class HttpRecordStore(val recorderConfig: HttpRecorderConfig, dropSession: Boolean = false)
    extends AutoCloseable
    with LogSupport {
  private val connectionPool = new SQLiteConnectionPool(DbConfig.ofSQLite(recorderConfig.sqliteFilePath))

  private def recordTableName = recorderConfig.recordTableName

  // Prepare a database table for recording HttpRecord
  connectionPool.executeUpdate(HttpRecord.createTableSQL(recordTableName))
  connectionPool.executeUpdate(
    s"create index if not exists ${recordTableName}_index on ${recordTableName} (session, requestHash)")
  // TODO: Detect schema change
  if (dropSession) {
    warn(s"Deleting old session records for session:${recorderConfig.sessionName}")
    connectionPool.executeUpdate(s"delete from ${recordTableName} where session = '${recorderConfig.sessionName}'")
  }

  private val requestCounter = scala.collection.mutable.Map.empty[Int, AtomicInteger]

  def resetCounter: Unit = {
    requestCounter.clear()
  }

  def findNext(request: Request): Option[HttpRecord] = {
    val rh = requestHash(request)

    // If there are multiple records for the same request, use the counter to find
    // n-th request, where n is the access count to the same path
    val counter  = requestCounter.getOrElseUpdate(rh, new AtomicInteger())
    val hitCount = counter.getAndIncrement()
    trace(s"findNext: request hash: ${rh} for ${request}, hitCount: ${hitCount}")
    connectionPool.queryWith(
      // Get the next request matching the requestHash
      s"select * from ${recordTableName} where session = ? and requestHash = ? order by createdAt limit 1 offset ?") {
      prepare =>
        prepare.setString(1, recorderConfig.sessionName)
        prepare.setInt(2, rh)
        prepare.setInt(3, hitCount)
    } { rs =>
      HttpRecord.read(rs).headOption
    }
  }

  def record(request: Request, response: Response): Unit = {
    val rh = requestHash(request)
    val entry = HttpRecord(
      recorderConfig.sessionName,
      requestHash = rh,
      method = request.method.toString(),
      destHost = recorderConfig.destAddress.hostAndPort,
      path = request.uri,
      requestHeader = request.headerMap.toMap,
      requestBody = request.contentString,
      responseCode = response.statusCode,
      responseHeader = response.headerMap.toMap,
      responseBody = response.contentString,
      createdAt = Instant.now()
    )
    connectionPool.withConnection { conn =>
      entry.insertInto(recordTableName, conn)
    }
  }

  override def close(): Unit = {
    connectionPool.stop
  }

  /**
    * Compute a hash key of the given HTTP request.
    * This value will be used for DB indexes
    */
  protected def requestHash(request: Request): Int = {
    val prefix = HttpRecorder.computeRequestHash(request, recorderConfig)

    val httpHeadersForHash = request.headerMap.toSeq.filterNot { x =>
      val key = x._1.toLowerCase(Locale.ENGLISH)
      recorderConfig.lowerCaseHeaderExcludePrefixes.exists(ex => key.startsWith(ex))
    }

    httpHeadersForHash match {
      case headers if headers.isEmpty => prefix.hashCode * 13
      case headers =>
        val headerHash = headers
          .map { x =>
            s"${x._1.toLowerCase(Locale.ENGLISH)}:${x._2}".hashCode
          }.reduce { (xor, next) =>
            xor ^ next // Take XOR to compute order-insensitive hash values.
          }
        prefix.hashCode * 13 + headerHash
    }
  }
}
