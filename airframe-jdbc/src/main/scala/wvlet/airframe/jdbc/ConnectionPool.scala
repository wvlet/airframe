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

import java.sql.{Connection, PreparedStatement, ResultSet}

import wvlet.log.LogSupport
import wvlet.log.io.IOUtil.withResource

object ConnectionPool {
  def apply(config: DbConfig): ConnectionPool = {
    val pool: ConnectionPool = config.`type` match {
      case "sqlite" => new SQLiteConnectionPool(config)
      case other =>
        new GenericConnectionPool(config)
    }
    pool
  }

  def newFactory: ConnectionPoolFactory = new ConnectionPoolFactory()
}

trait ConnectionPool extends LogSupport with AutoCloseable {
  def config: DbConfig

  def withConnection[U](body: Connection => U): U
  def withTransaction[U](body: Connection => U): U = {
    withConnection { conn =>
      conn.setAutoCommit(false)
      var failed = false
      try {
        body(conn)
      } catch {
        case e: Throwable =>
          // Need to set the failed flag first because the rollback might fail
          failed = true
          conn.rollback()
          throw e
      } finally {
        if (failed == false) {
          conn.commit()
        }
      }
    }
  }

  def stop: Unit

  override def close(): Unit = stop

  def executeQuery[U](sql: String)(handler: ResultSet => U): U = {
    withConnection { conn =>
      withResource(conn.createStatement()) { stmt =>
        debug(s"execute query: ${sql}")
        withResource(stmt.executeQuery(sql)) { rs =>
          handler(rs)
        }
      }
    }
  }
  def executeUpdate(sql: String): Int = {
    // TODO Add update retry
    withConnection { conn =>
      withResource(conn.createStatement()) { stmt =>
        debug(s"execute update: ${sql}")
        stmt.executeUpdate(sql)
      }
    }
  }

  def queryWith[U](preparedStatement: String)(body: PreparedStatement => Unit)(handler: ResultSet => U): U = {
    withConnection { conn =>
      withResource(conn.prepareStatement(preparedStatement)) { stmt =>
        body(stmt)
        debug(s"execute query: ${preparedStatement}")
        withResource(stmt.executeQuery) { rs =>
          handler(rs)
        }
      }
    }
  }

  def updateWith(preparedStatement: String)(body: PreparedStatement => Unit): Unit = {
    withConnection { conn =>
      withResource(conn.prepareStatement(preparedStatement)) { stmt =>
        body(stmt)
        stmt.executeUpdate()
      }
    }
  }

}
