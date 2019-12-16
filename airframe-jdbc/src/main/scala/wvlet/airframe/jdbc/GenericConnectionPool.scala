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

/**
  *
  */
class GenericConnectionPool(val config: DbConfig) extends ConnectionPool {

  protected val dataSource: HikariDataSource = {
    val connectionPoolConfig = new HikariConfig

    // Set default JDBC parameters
    connectionPoolConfig.setMaximumPoolSize(config.connectionPool.maxPoolSize) // HikariCP default = 10
    connectionPoolConfig.setAutoCommit(config.connectionPool.autoCommit)       // Enable auto-commit

    connectionPoolConfig.setDriverClassName(config.jdbcDriverName)
    config.user.foreach(u => connectionPoolConfig.setUsername(u))
    config.password.foreach(p => connectionPoolConfig.setPassword(p))

    config.`type` match {
      case "postgresql" =>
        if (config.postgres.useSSL) {
          connectionPoolConfig.addDataSourceProperty("ssl", "true")
          connectionPoolConfig.addDataSourceProperty("sslfactory", config.postgres.sslFactory)
        }
    }

    if (config.host.isEmpty) {
      throw new IllegalArgumentException(s"missing jdbc host: ${config}")
    }

    connectionPoolConfig.setJdbcUrl(
      s"jdbc:${config.`type`}://${config.host.get}:${config.jdbcPort}/${config.database}"
    )

    info(s"jdbc URL: ${connectionPoolConfig.getJdbcUrl}")
    new HikariDataSource(connectionPoolConfig)
  }

  override def withConnection[U](body: Connection => U): U = {
    val conn = dataSource.getConnection
    try {
      body(conn)
    } finally {
      // Return the connection to the pool
      conn.close
    }
  }

  override def stop: Unit = {
    info(s"Closing connection pool for ${config}")
    dataSource.close()
  }
}
