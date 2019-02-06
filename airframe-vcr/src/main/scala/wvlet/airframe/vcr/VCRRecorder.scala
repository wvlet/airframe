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
package wvlet.airframe.vcr
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

import com.twitter.finagle.http.{Request, Response, Status}
import wvlet.airframe.jdbc.{DbConfig, SQLiteConnectionPool}
import wvlet.airframe.tablet.jdbc.{ResultSetReader, SQLObjectMapper}
import wvlet.airframe.tablet.obj.ObjectTabletWriter

case class VCREntry(session: String,
                    requestHash: Int,
                    method: String,
                    uri: String,
                    code: Int,
                    header: Map[String, String],
                    requestBody: Option[String],
                    createdAt: Instant) {

  def toResponse: Response = {
    val r = Response(Status(code))
    header.foreach { x =>
      r.headerMap.set(x._1, x._2)
    }
    requestBody.foreach { body =>
      r.contentString = body
    }
    r
  }
}

/**
  * Recorder for HTTP server responses
  */
class VCRRecorder(vcrConfig: VCRConfig) extends AutoCloseable {
  private val connectionPool = new SQLiteConnectionPool(
    DbConfig.ofSQLite(s"${vcrConfig.folder}/${vcrConfig.sessionName}"))

  private def vcrTableName = vcrConfig.vcrTableName

  // Prepare a database table for recording VCREntry
  connectionPool.executeUpdate(SQLObjectMapper.createTableSQLFor[VCREntry](vcrTableName))
  connectionPool.executeUpdate(s"create index if not exists ${vcrTableName}_index (session, requestHash)")

  private val requestCounter = scala.collection.mutable.Map.empty[Int, AtomicInteger]

  def find(request: Request): Option[VCREntry] = {
    val requestHash = VCRRecorder.requestHash(request)
    val counter     = requestCounter.getOrElseUpdate(requestHash, new AtomicInteger())
    val hitCount    = counter.getAndIncrement()
    connectionPool.queryWith(
      // Get the
      s"select * from ${vcrTableName} where session = ? and requestHash = ? order by createdAt limit 1 offset ?") {
      prepare =>
        prepare.setString(1, vcrConfig.sessionName)
        prepare.setInt(2, requestHash)
        prepare.setInt(3, hitCount)
    } { rs =>
      // TODO: Migrate JDBC ResultSet reader to airframe-codec
      val reader = new ResultSetReader(rs)
      val writer = new ObjectTabletWriter[VCREntry]()
      reader.pipe(writer).headOption
    }
  }

  def record(request: Request, response: Response): Unit = {
    val requestHash = VCRRecorder.requestHash(request)
    val body        = if (response.content.isEmpty) None else Some(response.contentString)
    val entry = VCREntry(
      vcrConfig.sessionName,
      requestHash,
      method = request.method.toString(),
      uri = request.uri,
      code = response.statusCode,
      header = response.headerMap.toMap,
      requestBody = body,
      createdAt = Instant.now()
    )
    connectionPool.withConnection { conn =>
      SQLObjectMapper.insertRecord[VCREntry](conn, vcrTableName, entry)
    }
  }

  override def close(): Unit = {
    connectionPool.stop
  }
}

object VCRRecorder {

  /**
    * Compute a hash key of the given HTTP request.
    * This value will be used for DB indexes
    */
  def requestHash(request: Request): Int = {
    val content = request.getContentString()
    val prefix  = s"${request.method.toString().hashCode}:${request.uri.hashCode}:${content.hashCode}"

    val headerHash =
      request.headerMap
        .map { x =>
          s"${x._1}:${x._2}".hashCode
        }
        .reduce { (xor, next) =>
          xor ^ next // Take XOR to compute order-insensitive hash values.
        }
    prefix.hashCode + headerHash
  }

}
