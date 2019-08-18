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

import wvlet.airframe._
import wvlet.log.io.IOUtil.withResource
import wvlet.log.{Guard, LogSupport}

import scala.util.{Failure, Success, Try}

trait ConnectionPool extends LogSupport {
  def config: DbConfig

  def withConnection[U](body: Connection => U): U
  def stop: Unit

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

trait ConnectionPoolFactoryService {
  val connectionPoolFactory = bind[ConnectionPoolFactory]
    .onShutdown(_.close)

}

trait ConnectionPoolFactory extends Guard with AutoCloseable with LogSupport {

  private var createdPools = List.empty[ConnectionPool]

  /**
    * Use this method to add a precisely configured connection pool
    * @param pool
    * @return
    */
  def addConnectionPool(pool: ConnectionPool): ConnectionPool = {
    guard {
      // Register the generated pool to the list
      createdPools = pool :: createdPools
    }
    pool
  }

  /**
    *
    * @param config
    * @param pgConfig
    * @return
    */
  def newConnectionPool(config: DbConfig, pgConfig: PostgreSQLConfig = PostgreSQLConfig()): ConnectionPool = {
    val pool: ConnectionPool = config.`type` match {
      case "postgresql" => new PostgreSQLConnectionPool(config, pgConfig)
      case "sqlite"     => new SQLiteConnectionPool(config)
      case other =>
        throw new IllegalArgumentException(s"Unsupported database type ${other}")
    }
    addConnectionPool(pool)
  }

  override def close: Unit = {
    guard {
      createdPools.foreach { x =>
        Try(x.stop) match {
          case Success(u) => // OK
          case Failure(e) => warn(e)
        }
      }
      createdPools = List.empty
    }
  }
}
