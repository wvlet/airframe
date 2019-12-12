---
id: airframe-json
title: airframe-json: Pure-Scala JSON Parser
---

airframe-json is a pure-Scala json parser.

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-jmx_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-json_2.12/)

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-json" % "(version)"
```

### Parsing JSON
```scala
import wvlet.airframe.json.JSON

// Returns a JSON value object of the input JSON string 
val jsonValue = JSON.parse("""{"id":1, "name":"leo"}""")
```


### Mapping JSON into object

With airframe-codec, it supports JSON to object mapping.

Add airframe-codec to your dependency:
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-codec" % "(version)"
```

`MessageCodec` will create a JSON encoder and decorder for your objects:
```scala
import wvlet.airframe.codec.MessageCodec

case class Person(id:Int, name:String)

val p = Person(1, "leo")

val codec = MessageCodec.of[Person]
// Convert the object to JSON representation
val json = codec.toJson(p) // {"id":1, "name":"leo"}

// Read JSON as case class 
codec.unpackJson(json) // Some(Person(1, "leo"))

```
