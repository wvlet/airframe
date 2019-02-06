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

import com.twitter.finagle.http.{Request, Response}
import wvlet.airframe.jdbc.{DbConfig, SQLiteConnectionPool}

/**
  * Recorder for HTTP server responses
  */
class HttpRecordStore(val recorderConfig: HttpRecorderConfig) extends AutoCloseable {
  private val connectionPool = new SQLiteConnectionPool(DbConfig.ofSQLite(recorderConfig.sqliteFilePath))

  private def recordTableName = recorderConfig.recordTableName

  // Prepare a database table for recording HttpRecord
  connectionPool.executeUpdate(HttpRecord.createTableSQL(recordTableName))
  connectionPool.executeUpdate(
    s"create index if not exists ${recordTableName}_index on ${recordTableName} (session, requestHash)")
  // TODO: Detect schema change

  private val requestCounter = scala.collection.mutable.Map.empty[Int, AtomicInteger]

  def resetCounter: Unit = {
    requestCounter.clear()
  }

  def find(request: Request): Option[HttpRecord] = {
    val rh = requestHash(request)

    // If there are multiple records for the same request, use the counter to find
    // n-th request, where n is the access count to the same path
    val counter  = requestCounter.getOrElseUpdate(rh, new AtomicInteger())
    val hitCount = counter.getAndIncrement()
    connectionPool.queryWith(
      // Get the
      s"select * from ${recordTableName} where session = ? and requestHash = ? order by createdAt limit 1 offset ?") {
      prepare =>
        prepare.setString(1, recorderConfig.sessionName)
        prepare.setInt(2, rh)
        prepare.setInt(3, hitCount)
    } { rs =>
      // TODO: Migrate JDBC ResultSet reader to airframe-codec
      val b = Seq.newBuilder[HttpRecord]
      while (rs.next()) {
        b += HttpRecord.read(rs)
      }
      b.result().headOption
    }
  }

  def record(request: Request, response: Response): Unit = {
    val rh   = requestHash(request)
    val body = if (response.content.isEmpty) None else Some(response.contentString)
    val entry = HttpRecord(
      recorderConfig.sessionName,
      requestHash = rh,
      method = request.method.toString(),
      destHost = recorderConfig.destHostAndPort,
      path = request.uri,
      requestHeader = request.headerMap.toMap,
      responseCode = response.statusCode,
      responseHeader = response.headerMap.toMap,
      requestBody = body,
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
  def requestHash(request: Request): Int = {
    val content = request.getContentString()
    val prefix  = s"${request.method.toString()}:${recorderConfig.destHostAndPort}${request.uri}:${content.hashCode}"

    val headerHash =
      request.headerMap
        .filterNot(x => recorderConfig.headerExcludes(x._1))
        .map { x =>
          s"${x._1}:${x._2}".hashCode
        }
        .reduce { (xor, next) =>
          xor ^ next // Take XOR to compute order-insensitive hash values.
        }
    prefix.hashCode * 13 + headerHash
  }

}
