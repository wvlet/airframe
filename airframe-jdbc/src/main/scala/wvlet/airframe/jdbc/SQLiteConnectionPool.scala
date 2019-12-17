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

import java.io.File
import java.sql.{Connection, DriverManager}

import wvlet.log.Guard

/**
  * SQLite doesn't work well with HikariCP, so creating a simple one here
  */
class SQLiteConnectionPool(val config: DbConfig) extends ConnectionPool with Guard {
  private var conn: Connection = newConnection

  private def newConnection: Connection = {
    // Prepare parent db folder
    Option(new File(config.database).getParentFile).map { p =>
      if (!p.exists()) {
        info(s"Create db folder: ${p}")
        p.mkdirs()
      }
    }

    val jdbcUrl = s"jdbc:sqlite:${config.database}"
    info(s"Opening ${jdbcUrl}")
    // We need to explicitly load sqlite-jdbc to cope with SBT's peculiar class loader
    Class.forName(config.jdbcDriverName)
    val conn = DriverManager.getConnection(jdbcUrl)
    conn.setAutoCommit(true)
    conn
  }

  def withConnection[U](body: Connection => U): U = {
    guard {
      if (conn.isClosed) {
        conn = newConnection
      }
      // In sqlite-jdbc, we can reuse the same connection instance,
      // and we have no need to close the connection
      body(conn)
    }
  }

  def stop: Unit = {
    info(s"Closing connection pool for ${config}")
    conn.close()
  }
}
