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

import java.sql.Connection

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

case class PostgreSQLConfig(
    maxPoolSize: Int = 10,
    autoCommit: Boolean = true, // Enable auto-commit
    // SSL configuration for using RDS
    useSSL: Boolean = true,
    sslFactory: String = "org.postgresql.ssl.NonValidatingFactory"
)

/**
  *
  */
class PostgreSQLConnectionPool(val config: DbConfig, val pgConfig: PostgreSQLConfig = PostgreSQLConfig())
    extends ConnectionPool {
  private val dataSource: HikariDataSource = {
    val connectionPoolConfig = new HikariConfig

    // Set default JDBC parameters
    connectionPoolConfig.setMaximumPoolSize(pgConfig.maxPoolSize) // HikariCP default = 10
    connectionPoolConfig.setAutoCommit(pgConfig.autoCommit)       // Enable auto-commit

    connectionPoolConfig.setDriverClassName("org.postgresql.Driver")
    config.user.foreach(u => connectionPoolConfig.setUsername(u))
    config.password.foreach(p => connectionPoolConfig.setPassword(p))
    if (pgConfig.useSSL) {
      connectionPoolConfig.addDataSourceProperty("ssl", "true")
      connectionPoolConfig.addDataSourceProperty("sslfactory", pgConfig.sslFactory)
    }
    if (config.host.isEmpty) {
      throw new IllegalArgumentException(s"missing postgres jdbc host: ${config}")
    }
    connectionPoolConfig.setJdbcUrl(
      s"jdbc:postgresql://${config.host.get}:${config.port.getOrElse(5432)}/${config.database}")

    info(s"jdbc URL: ${connectionPoolConfig.getJdbcUrl}")
    new HikariDataSource(connectionPoolConfig)
  }

  def withConnection[U](body: Connection => U): U = {
    val conn = dataSource.getConnection
    try {
      body(conn)
    } finally {
      // Return the connection to the pool
      conn.close
    }
  }

  def stop {
    info(s"Closing connection pool for ${config}")
    dataSource.close()
  }
}
