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
import java.nio.charset.StandardCharsets
import java.sql.{Connection, ResultSet}
import java.time.Instant

import com.twitter.finagle.http.{Response, Status, Version}
import com.twitter.io.Buf
import com.twitter.io.Buf.{ByteArray, ByteBuffer}
import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.codec.{JSONCodec, MessageCodec}
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.recorder.HttpRecord.headerCodec

/**
  * HTTP response record that will be stored to the database
  */
case class HttpRecord(session: String,
                      requestHash: Int,
                      method: String,
                      destHost: String,
                      path: String,
                      requestHeader: Map[String, String],
                      requestBody: String,
                      responseCode: Int,
                      responseHeader: Map[String, String],
                      responseBody: String,
                      createdAt: Instant) {

  def summary: String = {
    s"${method}(${responseCode}) ${destHost}${path}: ${responseBody.substring(0, 30.min(responseBody.size))} ..."
  }

  def toResponse: Response = {
    val r        = Response(Version.Http11, Status.fromCode(responseCode))
    val rawBytes = responseBody.getBytes(StandardCharsets.UTF_8)
    r.content = Buf.ByteArray.Owned(rawBytes)
    r.contentLength = rawBytes.length
    responseHeader.foreach { x =>
      r.headerMap.set(x._1, x._2)
    }
    r
  }

  def insertInto(tableName: String, conn: Connection): Unit = {
    withResource(conn.prepareStatement(s"""|insert into "${tableName}" values(
          |?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
          |)
      """.stripMargin)) { prep =>
      // TODO Implement this logic in JDBCResultSetCodec
      prep.setString(1, session)
      prep.setInt(2, requestHash)
      prep.setString(3, method)
      prep.setString(4, destHost)
      prep.setString(5, path)
      prep.setString(6, JSONCodec.toJson(headerCodec.toMsgPack(requestHeader)))
      prep.setString(7, requestBody)
      prep.setInt(8, responseCode)
      prep.setString(9, JSONCodec.toJson(headerCodec.toMsgPack(responseHeader)))
      prep.setString(10, responseBody)
      prep.setString(11, createdAt.toString)

      prep.execute()
    }
  }
}

object HttpRecord {
  private[recorder] val headerCodec = MessageCodec.of[Map[String, String]]

  private[recorder] def createTableSQL(tableName: String): String =
    // TODO: Add a method to generate this SQL statement in airframe-codec
    s"""create table if not exists "${tableName}" (
       |  session string,
       |  requestHash string,
       |  method string,
       |  destHost string,
       |  path string,
       |  requestHeader string,
       |  requestBody string,
       |  responseCode int,
       |  responseHeader string,
       |  responseBody string,
       |  createdAt string
       |)
     """.stripMargin

  private def decodeJsonMap(json: String): Map[String, String] = {
    headerCodec.unpackMsgPack(StringCodec.toMsgPack(json)).getOrElse(Map.empty)
  }

  private[recorder] def read(rs: ResultSet): HttpRecord = {
    // TODO support reading json values embedded as string in airframe-codec
    HttpRecord(
      session = rs.getString(1),
      requestHash = rs.getInt(2),
      method = rs.getString(3),
      destHost = rs.getString(4),
      path = rs.getString(5),
      requestHeader = decodeJsonMap(rs.getString(6)),
      requestBody = rs.getString(7),
      responseCode = rs.getInt(8),
      responseHeader = decodeJsonMap(rs.getString(9)),
      responseBody = rs.getString(10),
      createdAt = Instant.parse(rs.getString(11))
    )
  }
}
