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
import wvlet.airframe.control.Control
import wvlet.log.{Guard, LogSupport}

/**
  * A factory for managing multiple connection pools and properly closes these pools upon shutdown
  */
class ConnectionPoolFactory extends Guard with AutoCloseable with LogSupport {
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
    * Add a new connection pool for a specific database
    * @param config
    * @return
    */
  def newConnectionPool(config: DbConfig): ConnectionPool = {
    addConnectionPool(ConnectionPool(config))
  }

  override def close: Unit = {
    guard {
      Control.closeResources(createdPools: _*)
      createdPools = List.empty
    }
  }
}
