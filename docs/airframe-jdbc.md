---
id: airframe-jdbc
title: airframe-jdbc: JDBC Connection Pool 
---

airframe-jdbc is a reusable JDBC connection pool implementation with Airframe. 

Currently we are supporting these databases:

- **sqlite**: SQLite
- **postgres**: PostgreSQL (e.g., [AWS RDS](https://aws.amazon.com/rds/))
- Generic JDBC drivers

Adding a new connection pool type would be easy. Your contributions are welcome.


## Usage
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-jdbc_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-jdbc_2.12/)

**build.sbt**

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-jdbc" % "(version)"
```


```scala
import wvlet.airframe._
import wvlet.airframe.jdbc._

// Import ConnectionPoolFactoryService 
trait MyDbTest extends ConnectionPoolFactoryService {
  // Create a new connection pool. The created pool will be closed automatically
  // after the Airframe session is terminated.
  val connectionPool = bind{ config:DbConfig => connectionPoolFactory.newConnectionPool(config) }

  // Create a new database
  connectionPool.executeUpdate("craete table if not exists test(id int, name text)")
  // Update the database with prepared statement
  connectionPool.updateWith("insert into test values(?, ?)") { ps =>
    ps.setInt(1, 1)
    ps.setString(2, "name")  
  }
  // Read ResultSet
  connectionPoo.executeQuery("select * from test") { rs =>
    // Traverse the query ResultSet here
    while (rs.next()) {
      val id   = rs.getInt("id")
      val name = rs.getString("name")
      println(s"read (${id}, ${name})")
    }
  }
}

...

// Configuring database
val d = newDesign
   // ConnectionPoolFactory should be a singleton so as not to create duplicated pools
  .bind[ConnectionPoolFactory].toSingleton
  .bind[DbConfig].toInstance(DbConfig.ofSQLite(path="mydb.sqlite"))

d.withSession { session =>
   val t = session.build[MyDbTest]
   
   // You can make queries using the connection pool
   t.connectionPool.executeQuery("select ...")
}
// Connection pools will be closed here

```

## Using PostgreSQL

For using RDS, configure DbConfig as follows:

```scala
val d = newDesign
  .bind[ConnectionPoolFactory].toSingleton
  .bind[DbConfig].toInstance(
    DbConfig.ofPostgreSQL(
      host="(your RDS address, e.g., mypostgres.xxxxxx.us-east-1.rds.amazonaws.com)",
      database="mydatabase"
    )
    .withUser("postgres")
    .withPassword("xxxxx")
  )
```

For accessing a local PostgreSQL without SSL support, disable SSL access like this:
```scala
val d = newDesign
  .bind[ConnectionPoolFactory].toSingleton
  .bind[DbConfig].toInstance(
    DbConfig.ofPostgreSQL(
      host="(your RDS address, e.g., mypostgres.xxxxxx.us-east-1.rds.amazonaws.com)",
      database="mydatabase"
    )
    .withUser("postgres")
    .withPassword("xxxxx"),
    PostgreSQLConfig(useSSL=false)
  )
```

## Creating multiple connection pools

You can create multiple connection pools with different configurations by using type aliases to DbConfig:

```scala
import wvlet.airframe._
import wvlet.airframe.jdbc._

object MultipleConnection {
  type MyDb1Config = DbConfig
  type MyDb2Config = DbConfig 
}

import MultipleConnection._

trait MultipleConnection extends ConnectionPoolFactoryService {
  val pool1 = bind{ c:MyDB1Config => connectionPoolFactory.newConnectionPool(c) }
  val pool2 = bind{ c:MyDB2Config => connectionPoolFactory.newConnectionPool(c) }
}

val d = newDesign
  .bind[ConnectionPoolFactory].toSingleton
  .bind[MyDb1Config].toInstance(DbConfig.ofSQLite(path="mydb.sqlite"))
  .bind[MyDb2Config].toInstance(DbConfig.ofPostgreSQL(database="mydatabase"))

``` 
