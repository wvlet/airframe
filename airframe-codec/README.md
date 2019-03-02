airframe-codec
====

airframe-codec is an [MessagePack](https://msgpack.org)-based schema-on-read data transcoder.

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

// Read msgpack data as object
codec.unpackMsgPack(msgpack) // Some(A(1, "leo"))


// Convert to JSON
val json = codec.toJson(a)   //  {"id":1,"name":"leo"}

codec.unpackJson(json)  // Some(A(1, "leo"))
```


- To create a custom codec, use MessageCodecFactory.
