---
id: airframe-jdbc
title: airframe-jdbc: JDBC Connection Pool 
---

airframe-jdbc is a reusable JDBC connection pool implementation with Airframe. 

Currently we are supporting these databases:

- **sqlite**: SQLite
- **postgres**: PostgreSQL (e.g., [AWS RDS](https://aws.amazon.com/rds/))
- Generic JDBC drivers

airframe-jdbc is wrapping [HikariCP](https://github.com/brettwooldridge/HikariCP)
jdbc connection pool for Scala.

## Usage
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-jdbc_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-jdbc_2.12/)

**build.sbt**

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-jdbc" % "(version)"
```

The basic usage is creating a new connection pool factory with `ConnectionPool.newFactory`, then pass database configuration `DbConfig` to `connectionPoolFactory.newConnectionPool(dbConfig)`:


```scala
import wvlet.airframe.jdbc._


// Create a new connection pool. The created pool will be closed when closing this factory.
val factory = ConnectionPool.newFactory

val dbConfig = DbConfig.ofSQLite(path = "mydb.sqlite")
val connectionPool = factory.newConnectionPool(dbConfig)

// Create a new database
connectionPool.executeUpdate("create table if not exists test(id int, name text)")
// Update the database with prepared statement
connectionPool.updateWith("insert into test values(?, ?)") { ps =>
  ps.setInt(1, 1)
  ps.setString(2, "name")  
}

// Read ResultSet
connectionPool.executeQuery("select * from test") { rs =>
  // Traverse the query ResultSet here
  while (rs.next()) {
    val id   = rs.getInt("id")
    val name = rs.getString("name")
    println(s"read (${id}, ${name})")
  }
}

// Close the created connection pools
factory.close()

```

### Using PostgreSQL

For using RDS, configure DbConfig as follows:

```scala
DbConfig.ofPostgreSQL(
  host="(your RDS address, e.g., mypostgres.xxxxxx.us-east-1.rds.amazonaws.com)",
  database="mydatabase"
)
.withUser("postgres")
.withPassword("xxxxx")
```

For accessing a local PostgreSQL without SSL support, disable SSL access like this:
```scala
DbConfig.ofPostgreSQL(
  host="(your RDS address, e.g., mypostgres.xxxxxx.us-east-1.rds.amazonaws.com)",
  database="mydatabase"
)
.withUser("postgres")
.withPassword("xxxxx"),
.withPostgreSQLConfig(PostgreSQLConfig(useSSL=false))
```

### Configure HikariCP Connection Pool

To add more configurations to the connection pool, use `withHikariConfig(...): 

```scala
DbConfig().withHikariConfig { (c: HikariConfig) =>
  // Add your configurations for HikariConfig 
  c.setIdleTimeout(...)
  c
}
```

The basic configurations (e.g., jdbc driver name, host, port, user, password, etc.) are already set, so you don't need to add them in withHikariConfig. 



## Using with Airframe DI
```scala

val d = newDesign
  .bind[DbConfig].toInstance(DbConfig(...))
  .bind[ConnectionPool].toProvider { (f:ConnectionPoolFactory, dbConfig:DbConfig) => f.newConnectionPool(dbConfig) }

d.build[ConnectionPool] { connectionPool =>
  // You can make queries using the connection pool
  connectionPool.executeQuery("select ...")
}
// Connection pools will be closed here

```

### Creating Multiple Connection Pools

You can create multiple connection pools with different configurations by using type aliases to DbConfig:

```scala
import wvlet.airframe._
import wvlet.airframe.jdbc._

object MultipleConnection {
  type MyDb1Config = DbConfig
  type MyDb2Config = DbConfig 
}

import MultipleConnection._

class MultipleConnection(f:ConnectionPoolFactory, c1:MyDB1Config, c2: MyDB2Config) {
  val pool1 = f.newConnectionPool(c1)
  val pool2 = f.newConnectionPool(c2) 
}

val d = newDesign
  .bind[MyDb1Config].toInstance(DbConfig.ofSQLite(path="mydb.sqlite"))
  .bind[MyDb2Config].toInstance(DbConfig.ofPostgreSQL(database="mydatabase"))
  
d.build[MultipleConnection] { c => ... }
// connections will be closed after the DI session closes   
``` 
