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

import com.zaxxer.hikari.HikariConfig
import wvlet.airframe.surface.secret

/**
  *
  */
case class DbConfig(
    // "type" is often used in Yaml, so we cannot change this parameter name
    `type`: String = "sqlite",
    host: Option[String] = None,
    database: String = "log/sample.db",
    private val port: Option[Int] = None,
    user: Option[String] = None,
    @secret password: Option[String] = None,
    private val driver: Option[String] = None,
    connectionPool: ConnectionPoolConfig = ConnectionPoolConfig(),
    // DBMS specific configurations will be added here so that we can describe
    // these configurations in the same YAML file
    postgres: PostgreSQLConfig = PostgreSQLConfig()
) {
  override def toString = {
    // TODO: pretty print
    s"DbConfig(${`type`},${host},$database,$port,$user,xxxxxx,${jdbcDriverName},${connectionPool})"
  }

  def jdbcUrl: String = {
    `type` match {
      case "sqlite" =>
        s"jdbc:sqlite:${database}"
      case _ =>
        s"jdbc:${`type`}://${host.getOrElse("localhost")}${port.map(p => s":${port}").getOrElse("")}/${database}"
    }
  }

  def withType(newType: String): DbConfig = {
    this.copy(`type` = newType)
  }

  def withHost(host: String): DbConfig = {
    this.copy(host = Some(host))
  }

  def withDatabase(database: String): DbConfig = {
    this.copy(database = database)
  }

  def withPort(port: Int): DbConfig = {
    this.copy(port = Some(port))
  }

  def jdbcPort: Int = {
    port.getOrElse {
      `type` match {
        case "postgresql" => 5432
        case "mysql"      => 3306
        case other =>
          throw new IllegalArgumentException(
            s"Unknown jdbc port for ${other}. Specify jdbc port number with withPort(...)"
          )
      }
    }
  }

  def withUser(user: String): DbConfig =
    this.copy(user = Option(user))

  def withPassword(password: String): DbConfig =
    this.copy(password = Some(password))

  def jdbcDriverName: String = {
    driver match {
      case Some(x) =>
        // Use custom driver name
        x
      case _ =>
        `type` match {
          case "sqlite"     => "org.sqlite.JDBC"
          case "postgresql" => "org.postgresql.Driver"
          case "mysql"      => "com.mysql.jdbc.Driver"
          case other =>
            throw new IllegalArgumentException(
              s"Unknown database type: ${other}. Specify jdbc driver name explicitly with withDriver(...)"
            )
        }
    }
  }

  def withDriver(driverClassName: String): DbConfig = {
    this.copy(driver = Some(driverClassName))
  }

  def withConnectionPoolConfig(connectionPoolConfig: ConnectionPoolConfig): DbConfig = {
    this.copy(connectionPool = connectionPoolConfig)
  }

  def withHikariConfig(configFilter: HikariConfig => HikariConfig): DbConfig = {
    this.copy(connectionPool = connectionPool.withHikariConfig(configFilter))
  }

  def withSQLiteConfig(dbFilePath: String): DbConfig = {
    this.copy(`type` = "sqlite", host = None, database = dbFilePath)
  }

  // Add PostgreSQL-specific configuration
  def withPostgreSQLConfig(postgresConfig: PostgreSQLConfig): DbConfig = {
    this.copy(`type` = "postgresql", postgres = postgresConfig)
  }
}

case class PostgreSQLConfig(
    // SSL configuration for using RDS
    useSSL: Boolean = true,
    sslFactory: String = "org.postgresql.ssl.NonValidatingFactory"
)

case class ConnectionPoolConfig(
    maxPoolSize: Int = 10,
    autoCommit: Boolean = true,
    hikariConfig: HikariConfig => HikariConfig = identity
) {
  override def toString: String = s"ConnectionPoolConfig(${maxPoolSize},${autoCommit})"

  def withHikariConfig(configFilter: HikariConfig => HikariConfig): ConnectionPoolConfig = {
    this.copy(hikariConfig = configFilter)
  }
}

object DbConfig {
  def of(dbType: String): DbConfig     = DbConfig(`type` = dbType)
  def ofSQLite(path: String): DbConfig = DbConfig().withSQLiteConfig(path)
  def ofPostgreSQL(host: String = "localhost", port: Int = 5432, database: String): DbConfig =
    DbConfig(host = Option(host), database = database, port = Some(port))
      .withPostgreSQLConfig(PostgreSQLConfig())
}
