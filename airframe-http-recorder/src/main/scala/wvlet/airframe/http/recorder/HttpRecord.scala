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
import java.sql.{Connection, ResultSet}
import java.time.Instant

import com.twitter.finagle.http.{Response, Status}
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
                      responseCode: Int,
                      responseHeader: Map[String, String],
                      requestBody: Option[String],
                      createdAt: Instant) {

  def toResponse: Response = {
    val r = Response(Status(responseCode))
    responseHeader.foreach { x =>
      r.headerMap.set(x._1, x._2)
    }
    requestBody.foreach { body =>
      r.contentString = body
    }
    r
  }

  def insertInto(tableName: String, conn: Connection): Unit = {
    withResource(conn.prepareStatement(s"""|insert into "${tableName}" values(
          |?, ?, ?, ?, ?, ?, ?, ?, ?, ?
          |)
      """.stripMargin)) { prep =>
      // TODO Implement this logic in JDBCResultSetCodec
      prep.setString(1, session)
      prep.setInt(2, requestHash)
      prep.setString(3, method)
      prep.setString(4, destHost)
      prep.setString(5, path)
      prep.setString(6, JSONCodec.toJson(headerCodec.toMsgPack(requestHeader)))
      prep.setInt(7, responseCode)
      prep.setString(8, JSONCodec.toJson(headerCodec.toMsgPack(responseHeader)))
      prep.setString(9, requestBody.getOrElse(""))
      prep.setString(10, createdAt.toString)

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
       |  responseCode int,
       |  responseHeader string,
       |  requestBody string,
       |  createdAt string
       |)
     """.stripMargin

  private def decodeJsonMap(json: String): Map[String, String] = {
    headerCodec.unpackMsgPack(JSONCodec.toMsgPack(json)).getOrElse(Map.empty)
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
      responseCode = rs.getInt(7),
      responseHeader = decodeJsonMap(rs.getString(8)),
      requestBody = {
        val s = rs.getString(9)
        if (s.isEmpty) None else Some(s)
      },
      createdAt = Instant.parse(rs.getString(10))
    )
  }
}
