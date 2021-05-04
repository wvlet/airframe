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
  "org.apache.hadoop"  % "hadoop-client"  % "3.3.0" 
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
val j1 = mapReader.read() // {"id":1,"name":"leo"}
val j2 = mapReader.read() // {"id":2,"name":"yui"} 
jsonReader.read() // null
jsonReader.close()
```
