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

import wvlet.airframe.AirframeSpec
import wvlet.airframe._
import wvlet.log.LogSupport

object ConnectionPoolFactoryTest {

  type MyDbConfig1 = DbConfig
  type MyDbConfig2 = DbConfig
  type MyDbConfig3 = DbConfig

}

import ConnectionPoolFactoryTest._

trait TestConnection extends ConnectionPoolFactoryService with LogSupport {
  lazy val pool1 = bind { c: MyDbConfig1 =>
    connectionPoolFactory.newConnectionPool(c)
  }
  lazy val pool2 = bind { c: MyDbConfig2 =>
    connectionPoolFactory.newConnectionPool(c)
  }
  lazy val pgPool = bind { c: MyDbConfig3 =>
    connectionPoolFactory.newConnectionPool(c, PostgreSQLConfig(useSSL = false))
  }

  def test(pool: ConnectionPool): Unit = {
    pool.executeUpdate("create table if not exists test(id int, name text)")
    pool.executeUpdate("insert into test values(1, 'leo')")
    pool.executeQuery("select * from test") { rs =>
      while (rs.next()) {
        val id   = rs.getInt("id")
        val name = rs.getString("name")
        logger.info(s"read (${id}, ${name})")
      }
    }

    // Reconnection test for SQLite
    if (pool.config.`type` == "sqlite") {
      pool.stop
    }

    pool.updateWith("insert into test values(?, ?)") { ps =>
      ps.setInt(1, 2)
      ps.setString(2, "yui")
    }
    pool.queryWith("select * from test where id = ?") { ps =>
      ps.setInt(1, 2)
    } { rs =>
      while (rs.next()) {
        val id   = rs.getInt("id")
        val name = rs.getString("name")
        logger.info(s"read with prepared statement: (${id}, ${name})")
      }
    }

    pool.executeUpdate("drop table if exists test")
  }

}

/**
  *
  */
class ConnectionPoolFactoryTest extends AirframeSpec {

  val d = newDesign
    .bind[ConnectionPoolFactory].toSingleton
    .bind[MyDbConfig1].toInstance(DbConfig(database = "target/test/mydb1.sqlite"))
    .bind[MyDbConfig2].toInstance(DbConfig(database = "target/test/mydb2.sqlite"))
    .bind[MyDbConfig3].toInstance(DbConfig(`type` = "postgresql", host = Option("localhost"), database = "travis_ci_test", user = Some("postgres")))

  "ConnectionPoolFactory" should {

    "use multiple SQLite configs" in {
      d.withSession { session =>
        val t = session.build[TestConnection]
        t.test(t.pool1)
        t.test(t.pool2)
      }
    }

    "use PostgreSQL connection pool" in {
      d.withSession { session =>
        val t = session.build[TestConnection]
        t.test(t.pgPool)
      }
    }

    "report error for unknown db type" in {
      intercept[IllegalArgumentException] {
        d.withSession { session =>
          val f = session.build[ConnectionPoolFactory]
          f.newConnectionPool(DbConfig(`type` = "superdb"))
        }
      }
    }

    "report error for missing postgresql host" in {
      intercept[IllegalArgumentException] {
        d.withSession { session =>
          val f = session.build[ConnectionPoolFactory]
          f.newConnectionPool(DbConfig(`type` = "postgresql", host = None))
        }
      }
    }

  }
}
