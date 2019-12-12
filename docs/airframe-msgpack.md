---
id: airframe-msgpack
title: airframe-msgpack: Pure-Scala MessagePack Parser
---

airframe-msgpack is a pure-Scala MessagePack reader and writer.  

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-jmx_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-msgpack_2.12/)

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-msgpack" % "(version)"
```

## Usage 

```scala
import wvlet.airframe.msgpack.spi.MessagePack

// Create a packer for writing MessagePack values
val packer = MessagePack.newBufferPacker

packer.packInt(10)
packer.packString("hello")
// ...

// Produce MessagePack byte array
val msgpack = packer.toByteArray


// Create an unpacker for reading MesagePack values 
val unpacker = MessagePack.newUnpacker(msgpack)
unpacker.unpackInt // 10
unpacker.unpackString // String
```
