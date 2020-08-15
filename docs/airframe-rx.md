---
id: airframe-rx
title: airframe-rx: ReactiveX interface
---

airframe-rx is a [ReactiveX](http://reactivex.io/) implementation for Scala and Scala.js.

airframe-rx is also used for
- Client/Server streaming for [Airframe gRPC](airframe-grpc.md)
- Interactive [rendering of DOM objects](https://github.com/wvlet/airframe/blob/master/airframe-http-rx/.js/src/main/scala/wvlet/airframe/http/rx/html/DOMRenderer.scala) (airframe-http-rx)

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-rx_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-rx_2.12/)

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-rx" % "(version)"
```


```scala
import wvlet.airframe.rx.Rx

// Create a new variable
val v = Rx.variable("World")
 
// Chain reactive operators 
val rx = v.map { x => s"Hello ${x}!" }

// Start monitoring changes of the Rx variable
rx.run { x => println(x) }
// "Hello World!" 

v := "Rx"  
// "Hello Rx!"
```
