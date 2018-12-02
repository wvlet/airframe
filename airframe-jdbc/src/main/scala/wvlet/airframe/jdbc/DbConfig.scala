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
package wvlet.airframe.jdbc

/**
  *
  */
case class DbConfig(
    // "type" is often used in Yaml, so we cannot change this parameter name
    `type`: String = "sqlite",
    host: Option[String] = None,
    database: String = "log/sample.db",
    port: Option[Int] = None,
    user: Option[String] = None,
    password: Option[String] = None
) {
  override def toString = s"DbConfig(${`type`},${host},$database,$port,$user,xxxxxx)"

  def withUser(user: String): DbConfig =
    DbConfig(`type`, host, database, port, Option(user), password)

  def withPassword(password: String): DbConfig =
    DbConfig(`type`, host, database, port, user, Option(password))
}

object DbConfig {
  def of(dbType: String): DbConfig     = DbConfig(`type` = dbType)
  def ofSQLite(path: String): DbConfig = DbConfig("sqlite", None, database = path, None, None, None)
  def ofPostgreSQL(host: String = "localhost", port: Int = 5432, database: String): DbConfig =
    DbConfig("postgresql", host = Option(host), database = database, port = Some(port))
}
