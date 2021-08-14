---
id: airframe-parquet
title: airframe-parquet: Parquet Columnar File Reader and Writer
---

airframe-parquet is a library for reading and writing for Scala objects using Parquet columnar data format.

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-parquet_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-parquet_2.12/)


```scala
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe-parquet" % "(version)"
  // Use your own hadoop version
  "org.apache.hadoop"  % "hadoop-client"  % "3.3.1",
  // [Optional] For supporting S3
  "org.apache.hadoop"  % "hadoop-aws"  % "3.3.1",
  // [Optional] For using custom AWS credential provider
  "software.amazon.awssdk" % "auth" % "2.17.18"
)
```


```scala
import wvlet.airframe.parquet.Parquet

case class MyEntry(id: Int, name:String)

// Writing objects to file
val writer = Parquet.newWriter[MyEntry](path = "data.parquet")
writer.write(MyEntry(1, "leo"))
writer.write(MyEntry(2, "yui"))
// Ensure writing entries to the file
writer.close()

// Reading Parquet data as objects
val reader = Parquet.newReader[MyEntry](path = "data.parquet")
val e1 = reader.read() // MyEntry(1,"leo")
val e2 = reader.read() // MyEntry(2,"yui")
reader.read() // null
reader.close()

// Reading records as Map[String, Any]
val mapReader = Parquet.newReader[Map[String, Any]](path = "data.parquet")
val m1 = mapReader.read() // Map("id"->1, "name" -> "leo")
val m2 = mapReader.read() // Map("id"->2, "name" -> "yui")
mapReader.read() // null
mapReader.close()

// Reading records as Json
import wvlet.airframe.json.Json
val jsonReader = Parquet.newReader[Json](path = "data.parquet")
val j1 = jsonReader.read() // {"id":1,"name":"leo"}
val j2 = jsonReader.read() // {"id":2,"name":"yui"} 
jsonReader.read() // null
jsonReader.close()
```

### Using with AWS S3

airframe-parquet uses HadoopFileSystem for reading data from S3.
hadoopConf needs to be configured for AWS authentication.

```scala
import org.apache.hadoop.conf.Configuration

val conf = new Configuration()
// Option 1: Using AWS keys
conf.set("fs.s3a.access.key", "...")
conf.set("fs.s3a.secret.key", "...")

// Option 2: Using a custom AWS credential provider implementing com.amazonaws.auth.AWSCredentialsProvider
conf.set("fs.s3a.aws.credentials.provider", "com.amazonaws.auth.DefaultAWSCredentialsProviderChain")

// Use s3a:// prefix to specify an S3 path, and pass hadoopConf
Parquet.newReader[MyEntry](path = "s3a://my-bucket/data.parquet", hadoopConf = conf)
```

For other configuration parameters, see also [hadoop-aws](https://hadoop.apache.org/docs/stable/hadoop-aws/tools/hadoop-aws/index.html) documentation.

## Querying Parquet with A Simple SQL

To apply column projection and predicate filtering, you can use SQL statements. The syntax of SQL is `select column1, column2, ... from _ where (column condition)`. The input table name must be just `_` (underscore). The where clause condition supports only a limited set of predicates, `=`, `!=`, `<`, `>`, `<=`, `>=`, `BETWEEN`, `OR`, `AND`, `IS NULL`, `IS NOT NULL`, etc., where the left operator is a column name.

Projecting columns:
```scala
// Selecting a subset of columns with SQL
val reader = Parquet.query[Json](path = "data.parquet", sql = "select id from _")
reader.read() // {"id":1}
reader.read() // {"id":2}
reader.read() // null
```

Filtering records:
```scala
// Selecting a subset of columns with SQL
val reader = Parquet.query[Json](path = "data.parquet", sql = "select * from _ where id = 2")
reader.read() // {"id":2}
reader.read() // null
```

## Column Projection with Model Classes

If you need to read only a subset of columns, use a model class that has fewer parameters from the original model class. The Parquet reader will access only to the column blocks of the specified column in the model class parameters:

```scala
case class MyRecord(p1:Int, p2: String, p3:Boolean)

val writer = Parquet.newWriter[MyRecord](path = "record.parquet")
writer.write(...)
writer.close()

case class MyRecordProjection(p1:Int, p3:Boolean)
val reader = Parquet.newReader[MyRecordProjection](path = "record.parquet")

// Only p1 and p3 columns will be read from the Parquet file
reader.read() // MyRecordProjection(p1, p3)
reader.close()
```


## Applying Row Group Filter with Parquet FilterApi

Parquet can skip reading records by using row group filters.
You can use [FilterAPI of parquet-mr](https://github.com/justcodeforfun/parquet-mr/blob/master/parquet-column/src/main/java/org/apache/parquet/filter2/predicate/FilterApi.java) to build such a filter:

```scala
import org.apache.parquet.filter2.compat.FilterCompat
import org.apache.parquet.filter2.predicate.FilterApi

// Describing filter condition using parquet-mr FilterApi
val filter = FilterCompat.get(
  FilterApi.eq(
    FilterApi.intColumn("id"),
    Integer.valueOf(100) // Need to use Java primitive values
  )
)

val reader = Parquet.newReader[MyEntry](
  path = "data.parquet",
  // Set your filter here
  config = _.withFilter(filter)
)
```

## Read Column Statistics

```scala
// Read Parquet metadat to get column statistics
val stats: Map[String, ColumnStatistics] = Parquet.readStatistics("data.parquet")
// Map(id -> ColumnStatistics(numNulls = Some(0), uncompressedSize = Some(..), .., minValue = Some(1), maxValue = Some(2)), ... )
```
