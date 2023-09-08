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
import wvlet.airframe.codec.*
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.recorder.HttpRecord.headerCodec
import wvlet.airframe.http.{Http, HttpStatus}
import wvlet.log.LogSupport

import java.sql.{Connection, ResultSet}
import java.time.Instant

/**
  * HTTP response record that will be stored to the database
  */
case class HttpRecord(
    session: String,
    requestHash: Int,
    method: String,
    destHost: String,
    path: String,
    requestHeader: Seq[(String, String)],
    requestBody: String,
    responseCode: Int,
    responseHeader: Seq[(String, String)],
    responseBody: String,
    createdAt: Instant
) {
  def summary: String = {
    s"${method}(${responseCode}) ${destHost}${path}: ${responseBody.substring(0, 30.min(responseBody.size))} ..."
  }

  def toResponse: Response = {
    var r = Http.response(HttpStatus.ofCode(responseCode))
    responseHeader.foreach { x => r = r.withHeader(x._1, x._2) }

    // Decode binary contents with Base64
    val contentBytes = HttpRecordStore.decodeFromBase64(responseBody)
    r = r
      .withContent(contentBytes)
      .withContentLength(contentBytes.length)
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

object HttpRecord extends LogSupport {
  private[recorder] val headerCodec = MessageCodec.of[Seq[(String, String)]]
  private[recorder] val recordCodec = MessageCodec.of[HttpRecord]
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

  private[recorder] def read(rs: ResultSet): Seq[HttpRecord] = {
    val resultSetCodec = JDBCCodec(rs)
    resultSetCodec
      .mapMsgPackMapRows(msgpack => recordCodec.unpackBytes(msgpack))
      .filter(_.isDefined)
      .map(_.get)
      .toSeq
  }
}
