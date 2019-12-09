---
id: airframe-codec
layout: docs
title: airframe-codec: Schema-On-Read Object Serializer
---

airframe-codec is an [MessagePack](https://msgpack.org)-based schema-on-read data transcoder for Scala and Scala.js.

With airframe-codec you can:
- Encode Scala objects (e.g., case classes, collection, etc.) into MessagePack format, and decode it. Object serialization/deserialization.
- Convert JDBC result sets into MessagePack
- Add you custom codec (implementing pack/unpack)
- You can use airframe-tablet is for reading CSV/TSV/JSON/JDBC data etc.    

airframe-codec supports schema-on-read data conversion.
For example, even if your data is string representation of integer values, e.g., "1", "2, "3", ..., 
airframe-codec can convert it into integers if the target schema (e.g., objects) requires integer values. 


## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-codec_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-codec_2.12/)

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-codec" % "(version)"
```


```scala
import wvlet.airframe.codec.MessageCodec


case class A(id:Int, name:String)

// Automatically generate a codec (MessagePack-based reader/writer) for A
val codec = MessageCodec.of[A]

// Convert an object into msgpack binary
val a = A(1, "leo")
val msgpack = codec.toMsgPack(a)

// Read MsgPack data as object (exception-free)
codec.unpackMsgPack(msgpack) // Some(A(1, "leo")) or None
// MsgPack -> Object (MessageCodecException will be thrown when parsing is failed)
codec.fromMsgPack(msgpack) // A(1, "leo")


// Convert to JSON
val json = codec.toJson(a) // {"id":1,"name":"leo"}

// Exception-free json parser
codec.unpackJson(json) // Some(A(1, "leo")) or None

// JSON -> Object (MessageCodecException will be thrown when parsing is failed)
codec.fromJson(json) // A(1, "leo")
```

- To create a custom codec, use MessageCodecFactory.


## Populating missing fields with zero values

When mapping data into objects, if some parameter values are missing in the input data, airframe-codec will try to populate these missing parameters with the default values of the target types. This default parameter mapping is defined in `Zero.zeroOf[X]`. For example, if the target type is Int, `zeroOf[Int]` will be 0, and for String `zeroOf[String]` is `""` (empty string).

___Default Values___

| X: type | zeroOf[X] | Notes |
|------|---------|----|
| Int  | 0       |
| Long  | 0L       |
| String | "" |
| Boolean | false |
| Option[X] | None |
| Float | 0.0f |
| Double | 0.0 |
| Array[X] | empty array |
| Seq[X] | Seq.empty |
| class A(p1:P1 = v1, ...) | A(p1 = v1, ...) | default parameter value|
| class A(P1, ...) | A(zeroOf[P1], ...) | zero value of each parameter |

## Strict Mapping

If you need to ensure the presence of some parameter values, add `@requried` annotation to the target object parameters. If some necessary parameter is missing, MessageCodecException will be thrown:

```scals
import wvlet.airframe.surface.required

case class Person(@required id:String, @requried name:String)

val codec = MessageCodec.of[Person]

// Throws MessageCodecException (MISSING_PARAMETER) exception as id value is not found
codec.fromJson("""{"name":"Peter"}""")

```
