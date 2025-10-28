---
id: airframe-parquet
title: airframe-parquet: Parquet Columnar File Reader and Writer
---

airframe-parquet is a library for reading and writing for Scala objects using Parquet columnar data format.

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-parquet_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-parquet_2.12/)

**Note**: Starting from version 24.x, airframe-parquet no longer requires explicit Hadoop dependencies for local file operations. It uses `NioInputFile`/`LocalOutputFile` which work with `java.nio.file.Path` directly. This dramatically reduces dependency size (85%+ reduction) and simplifies usage.

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-parquet" % "(version)"
```

For local file operations, no additional dependencies are needed. The library works out of the box with local filesystems.


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

// Writing dynamically generated records
import org.apache.parquet.schema._
// Create a Parquet schema
val schema = new MessageType(
  "MyEntry",
  Types.required(PrimitiveTypeName.INT32).named("id"),
  Types.optional(PrimitiveTypeName.BINARY).as(stringType).named("name")
)
// Create a record writer for the given schema
val recordWriter = Parquet.newRecordWriter(path = "record.parquet", schema = schema)
// Write a record using Map (column name -> value)
recordWriter.write(Map("id" -> 1, "name" -> "leo"))
// Write a record using JSON object
recordWriter.write("""{"id":2, "name":"yui"}""")
// Write a record using Array
recordWriter.write(Seq(3, "aina"))
// Write a record using JSON array
recordWriter.write("""[4, "xxx"]""")
recordWriter.close()


// In case you need to write dynamic recoreds containing case classes,
// register the Surfaces of these classes
case class Nested(id:Int, entry:MyEntry)
val nestedRecordWriter = Parquet.newRecordWriter(
  path = "nested.parquet",
  // You can build a Parquet schema matching to Surface
  schema = Parquet.toParquetSchema(Surface.of[Nested]),
  knownSurfaces = Seq(Surface.of[MyEntry]) // required to serialize MyEntry
)

// Write dynamic records
nestedRecordWriter.write(Map("id" -> 1, "entry" -> MyEntry(1, "yyy"))
nestedRecordWriter.write(Map("id" -> 2, "entry" -> MyEntry(2, "zzz"))
nestedRecordWriter.close()
```

### Using with AWS S3 and Remote Filesystems

airframe-parquet now uses `NioInputFile` which works with any Java NIO FileSystem provider, including S3.

For S3 support, you can use AWS's Java NIO FileSystem SPI implementation:

```scala
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe-parquet" % "(version)",
  // For S3 support via Java NIO FileSystem
  "software.amazon.awssdk" % "s3" % "2.31.78"
)
```

Then use S3 paths directly with the NIO FileSystem:

```scala
import java.net.URI
import java.nio.file.{FileSystems, Path}

// Create S3 FileSystem
val s3Uri = new URI("s3://my-bucket/")
val s3FileSystem = FileSystems.newFileSystem(s3Uri, Map(
  "aws.region" -> "us-east-1"
  // Add other AWS configuration as needed
).asJava)

// Use S3 paths directly
val s3Path = s3FileSystem.getPath("/data.parquet")
val reader = Parquet.newReader[MyEntry](path = s3Path.toString)
```

For AWS credential configuration, see the [AWS SDK for Java documentation](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html).

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
You can use [FilterAPI of parquet-mr](https://github.com/justcodeforfun/parquet-mr/blob/main/parquet-column/src/main/java/org/apache/parquet/filter2/predicate/FilterApi.java) to build such a filter:

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
