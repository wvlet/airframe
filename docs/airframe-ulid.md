---
id: airframe-ulid
title: airframe-ulid: Lexicographically sortable UUID
---

airframe-ulid is an [ULID](https://github.com/ulid/spec) generator for Scala and Scala.js.

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-ulid_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-ulid_2.12/)

__build.sbt__
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-ulid" % "(version)"

# For Scala.js
libraryDependencies += "org.wvlet.airframe" %%% "airframe-ulid" % "(version)"
```


```scala
import wvlet.airframe.ulid.ULID

// Generate a new ULID
val ulid = ULID.newULID

# ULID.toString produces the String representation of ULID
println(ulid)             // 01F3J0G1M4WQRBHGZ6HCF6JA0K
println(ulid.epochMillis) // 1618733434500

// Parse the string representation of ULIDs 
val ulid2 = ULID.fromString("01F3J0G1MD4QX7Z5QB148QNH4R")
ulid2.epochMillis // 1618733434509

// Parse the binary representation of ULIDs
ULID.fromBytes(...)

// Create an ULID from a given unixtime (48-bits, milliseconds) and a random value (80-bits) 
ULID.of(unixTimeMillis, randHi, randLow)
```


ULID.newULID will produce [monotonically increasing ULIDs](https://github.com/ulid/spec#monotonicity) in a thread-safe manner:

```
01F3HZ9V4BHVHJMMETE0MFBQKH
01F3HZ9V4BHVHJMMETE0MFBQKJ
01F3HZ9V4BHVHJMMETE0MFBQKK
01F3HZ9V4BHVHJMMETE0MFBQKM
01F3HZ9V4BHVHJMMETE0MFBQKN
01F3HZ9V4C8SWD21A4SCM4NMD8
...
```


ULID can be serialized and deserialized with [airframe-codec](airframe-codec.md): 
```scala
case class Transaction(id:ULID, status:String)

val codec = MessageCodec.of[Transaction]

val json = codec.toJson(Transaction(id = ULID.newULID, status = "committed")) 
// {"id":"01F3HZ9V454BC466FPDSBTZ64G", "status":"committed"}

val txx = codec.fromJson(json)
// Transaction(01F3HZ9V454BC466FPDSBTZ64G,committed)
```

## Performance

airframe-ulid can produce 5 million ULIDs / sec. As of April 2021, airframe-ulid is the fastest ULID generator in Scala: 

- [scala-ulid (by Chatwork)](https://github.com/chatwork/scala-ulid)

```scala
$ ./sbt
> benchmark/run bench ulid

Benchmark                    Mode  Cnt        Score        Error  Units
Airframe.generateMonotonic  thrpt   10  5514410.915 ± 534378.141  ops/s
Chatwork.generate           thrpt   10  2063919.975 ±  62641.190  ops/s
Chatwork.generateMonotonic  thrpt   10  4715112.355 ± 680190.587  ops/s
UUID.generate               thrpt   10  3056452.879 ± 121768.161  ops/s
```


- [Source Code at GitHub](https://github.com/wvlet/airframe/tree/master/airframe-ulid)
