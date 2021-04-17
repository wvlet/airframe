airframe-ulid
====

ULID is a lexicographically sortable UUID https://github.com/ulid/spec.
- [Source Code at GitHub](https://github.com/wvlet/airframe/tree/master/airframe-ulid)

# Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-ulid_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-ulid_2.12/)

__build.sbt__
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-ulid" % "(version)"

# For Scala.js
libraryDependencies += "org.wvlet.airframe" %%% "airframe-ulid" % "(version)"
```

```scala
import wvlet.airframe.ulid.ULID

# Generate a new ULID
val ulid = ULID.newULID

# ULID.toString produces the String representation of ULID
println(ulid)             // 01EF92SZENH2RVKMHDMNFX6FJG
println(ulid.epochMillis) // 1596959030741
```
