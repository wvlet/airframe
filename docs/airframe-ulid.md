---
id: airframe-ulid
title: airframe-ulid: ULID Generator
---

airframe-ulid is an [ULID](https://github.com/ulid/spec) generator for Scala and Scala.js. 

ULID (Universally Unique Lexicographically Sortable Identifier) has the following good characteristics:

- 128-bit compatibility with UUID
- 1.21e+24 unique ULIDs per millisecond
- Lexicographically sortable
- Canonically encoded as a 26 character string, as opposed to the 36 character UUID
Uses Crockford's base32 for better efficiency and readability (5 bits per character)
- Case insensitive
- No special characters (URL safe)
- Monotonic sort order (correctly detects and handles the same millisecond)

[Source code of airframe-ulid at GitHub](https://github.com/wvlet/airframe/tree/master/airframe-ulid)

## ULID Format

The ULID is a 26-character string encoded with [Crockford's Base32](https://www.crockford.com/base32.html), which excludes letters like I, L, O, and U to avoid confusion.

```
 01AN4Z07BY      79KA1307SR9X4MV3

|----------|    |----------------|
 Timestamp          Randomness
   48bits             80bits
```

### Timestamp

- 48-bit integer
- UNIX-time in milliseconds
- Won't run out of space until the year 10889 AD.

### Randomness

- 80 bits
- Cryptographically secure random value.

### Binary Format

ULIDs can be encoded as 128-bit values, using network-byte order (big-endian, MSB first). In JVM, it can be two 64-bit Long values, or a byte array of size 16. Technically, 26 characters of Crockford's Base32 can represent 130-bit values, but ULID strictly uses 128 bits, so the largest ULID string is `7ZZZZZZZZZZZZZZZZZZZZZZZZZ`, the unix time of which is 281474976710655.

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-ulid_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-ulid_2.12/)

__build.sbt__
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-ulid" % "(version)"

// For Scala.js
libraryDependencies ++= "org.wvlet.airframe" %%% "airframe-ulid" % "(version)"
```

ULID can be generated with `ULID.newULID` method:

```scala
import wvlet.airframe.ulid.ULID

// Generate a new ULID
val ulid: ULID = ULID.newULID

// ULID.toString produces the String representation of ULID
println(ulid)             // 01F3J0G1M4WQRBHGZ6HCF6JA0K
println(ulid.epochMillis) // 1618733434500

// You can generate a ULID string at ease with newULIDString
ULID.newULIDString

// Parse the string representation of ULIDs 
val ulid2 = ULID.fromString("01F3J0G1MD4QX7Z5QB148QNH4R")
ulid2.epochMillis // 1618733434509

// Parse the binary representation of ULIDs
ULID.fromBytes(...)


// Create an ULID from a given unixtime (milliseconds)
val unixTimeMillis = System.currentTimeMillis() - 1000
ULID.ofMillis(unixTimeMillis)

// Create an ULID from a given unixtime (48-bits, milliseconds) and a random value (80-bits) 
ULID.of(unixTimeMillis, randHi, randLow)

```


ULID.newULID will produce [monotonically increasing ULIDs](https://github.com/ulid/spec#monotonicity) in a thread-safe manner. If ULIDs are generated within the same millisecond, airframe-ulid will increment the random part of ULID by 1 to enforce the ordering:

```
01F3HZ9V4BHVHJMMETE0MFBQKH
01F3HZ9V4BHVHJMMETE0MFBQKJ // <- random part was incremented by 1
01F3HZ9V4BHVHJMMETE0MFBQKK // <- random part was incremented by 1
01F3HZ9V4BHVHJMMETE0MFBQKM // <- random part was incremented by 1
01F3HZ9V4BHVHJMMETE0MFBQKN // <- random part was incremented by 1
01F3HZ9V4C8SWD21A4SCM4NMD8 // <- millisecond is changed
...
```

Empirically, generating monotonically increasing ULIDs will reduce the probability of having ULID conflicts than using completely random ULIDs, according to [this report](https://medium.com/zendesk-engineering/how-probable-are-collisions-with-ulids-monotonic-option-d604d3ed2de).

If you use multiple machines, however, there is no guarantee that generated ULIDs will be monotonically increasing values. We recommend having a single coordinator to generate ULIDs if you need strict ordering.


### Serialization

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

```scala
$ ./sbt
> benchmark/run bench ulid

Benchmark                    Mode  Cnt        Score        Error  Units
Airframe.generateMonotonic  thrpt   10  5514410.915 ± 534378.141  ops/s
Chatwork.generate           thrpt   10  2063919.975 ±  62641.190  ops/s
Chatwork.generateMonotonic  thrpt   10  4715112.355 ± 680190.587  ops/s
UUID.generate               thrpt   10  3056452.879 ± 121768.161  ops/s
```

Comparison targets:
- [scala-ulid (by Chatwork)](https://github.com/chatwork/scala-ulid)
- Java's UUID.randomUUID()

[Benchmark code](https://github.com/wvlet/airframe/blob/master/airframe-benchmark/src/main/scala/wvlet/airframe/benchmark/ulid/ULIDBenchmark.scala) using JMH.


### Scala.js-specific Note

For Scala.js 1.10.0 or later, ULID generator uses scalajs-java-securerandom library for generating ULIDs using a Cryptographically-Secure-Pseudo-Random-Number-Generator (CSPRNG).
If none of Node.js crypto module or Web Crypto API in Web browsers is available, airframe-ulid falls back to a non-secure random generator, which is useful only for testing purpose at JSDOM environment, which doesn't support crypto module.
