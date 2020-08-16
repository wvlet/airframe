---
id: airframe-rx
title: airframe-rx: ReactiveX interface
---

airframe-rx is a lightweight [ReactiveX](http://reactivex.io/) implementation for Scala and Scala.js, which can be used for:
- Streaming support of [Airframe gRPC](airframe-rpc.md)
- [Interactive rendering of DOM objects](https://github.com/wvlet/airframe/blob/master/airframe-http-rx/.js/src/main/scala/wvlet/airframe/http/rx/html/DOMRenderer.scala) (airframe-http-rx)
- etc.


## Why Reactive Programming?

Reactive programming is a model of event-based processing. If you want to keep monitoring an event and write a chain of actions after observing some changes of the event, reactive programming is the right choice for you. 

### Frontend Programming

When writing an Web UI, we usually need to change DOM elements based on the state of some variable. This example will update the contents of DOM if `counter` variable is updated: 

```scala
import wvlet.airframe.rx.Rx

val counter: RxVar[Int] = Rx.variable(1)

val rx: Rx[RxElement] = counter.map { x =>
  div(s"count: ${x}")
}

// This will render:
// <div>count: 1</div>
DOMRenderer.render(rx)

// Update the counter variable
counter := 2
// Then, the above DOM will be rewritten to:
// <div>count: 2</div>
``` 

`Rx[A]` represents a reactive component which will be updated if there is any change in its upstream operators. In this example, we are chaining actions (map operator, etc.) based on the current state of `counter` variable. These actions are observing the state of the variable, and if the `counter` variable is updated, the registered actions will be triggered. [airframe-http-rx](airframe-http-rx.md) uses this pattern a lot to build flexible UI code in [Scala.js](https://www.scala-js.org), which will be compiled to JavaScripts so that we can use Scala for web browsers.

### Backend Programming

As an example of server-side programming, [gRPC](https://grpc.io/), which supports client and server-side streaming, is a good example of reactive programming. By using [Airframe gRPC](airframe-rpc.md), you can create a gRPC server, which returns multiple String messages as a stream:
```scala
import wvlet.airframe.http.RPC
import wvlet.airframe.rx.Rx

@RPC
trait MyApi {
  // Sending multiple String messages from the server as a stream
  def serverStreaming(name:String): Rx[String] = {
     Rx.sequence("Hello", "See You").map(msg => s"${msg} ${name}!")
  }
}
```

A gRPC client can receive a sequence of String messages from the server, and do some action as it receives a new message:

```scala
// As we receive a new message from the server, print the received message:
rpcClient.MyApi.serverStreaming("RPC").map { message => 
  println(message)
}
// prints "Hello RPC!"
// prints "See You RPC!"
```

----

These two examples are non-blocking code, which means that the subsequent code can be processed while we are waiting for updates of the observed events. If you use reactive components like `Rx[A]`, writing interactive UIs and RPC clients/servers becomes much easier than managing threads and event update signals by yourself. 

For more interested readers, visit [ReactiveX](http://reactivex.io/) web page. You can find various types of event-based processing patterns. You will notice that these stream processing operators are quite similar to [Scala collection library operators](https://docs.scala-lang.org/overviews/collections/trait-traversable.html), such as map, flatMap, filter, zip, etc. If you are already familiar to Scala, it would be easy to learn reactive programming as well.


## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-rx_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-rx_2.12/)

__build.sbt__

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-rx" % "(version)"
```

For Scala.js, use `%%%`: 
```scala
libraryDependencies += "org.wvlet.airframe" %%% "airframe-rx" % "(version)"
```

### Subscribing Variable Changes

```scala
import wvlet.airframe.rx.Rx

// Create a new variable
val v = Rx.variable("World")
 
// Chain reactive operators 
val rx = v.map { x => s"Hello ${x}!" }

// Start subscribing changes of the Rx variable
val c = rx.subscribe { x => println(x) }
// prints "Hello World!" 

// Setting a new value to the variable 
v := "Rx"  
// prints "Hello Rx!"

// Call cancel to stop the subscription
c.cancel
```

## Rx Operators

### Creating Rx 

- Rx.single
- Rx.sequence
- Rx.variable
- Rx.optionVariable

### Transforming Rx

- map
- flatMap

### Filtering Rx

- filter
- lastOption

### Combining Rx 

- concat
- zip
- join

### Error Handling Operators

- recover
- recoverWith

### Utility Operators

- subscribe
- run
- toOption


## Design of Airframe Rx

We built Airframe Rx with the following designs in our mind: 

- Isolating operators `Rx[A]` and their execution
- Supporting cancellation of the event subscription
- Supporting Scala.js for Web UI programming
- Minimizing the learning cost 
- Minimizing extra library dependencies

Airframe Rx is a tiny library that supports Scala 2.11, 2.12, 2.13, and Scala.js. It only has a dependency to [airframe-log](airframe-log.md) for the debug logging purpose, so there is no difficulty in using it in Scala.js, which cannot compile some Java-based libraries. `Rx[A]` interface itself has no execution code, and it uses `RxRunner` for processing Rx operators, so adding your own custom executor for evaluating `Rx[A]` is also possible. The event processing methods of `Rx[A]` is almost the same with Scala collection library (e.g., map, flatMap, filter, etc.), so there is nothing much to learn to start using Airframe Rx.

In Scala `Future[A]`, which represents a value of type _A_ that will be available in future, can be used for writing asynchronous code. By using the existing Future libraries, such as [scala.concurrent.Future](https://docs.scala-lang.org/overviews/core/futures.html) or [Twitter's Future](https://twitter.github.io/finagle/guide/Futures.html), etc., users can concatenate a sequence of operators for processing data without blocking the code execution. So, while waiting the data from remote sources, other tasks can be processed in parallel.

One of the drawbacks of Future is that cancelling already started execution is not straightforward. Cancelling execution is important if we neet to stop feeding streams or stop updating DOM elements when the user moves to a different page. [Monix](https://monix.io) has implemented a cancelable task abstraction, denoted _Task[A]_, which is similar to Future, but isolates operators and their executions so that we can have more control over the stream processing flows. Monix is targeting high-performant asynchronous event-based programming, and it has added many useful features over time. 

In our use cases, however, requiring Monix or even its small submodule monix-reactive was overkill and incurs additional learning cost about Monix itself. In addition, there was a concern that it would cause dependency problems to Airframe RPC and Rx users. Implementing `Rx` interface was not a big deal as it is just a set of operator definitions similar to Scala's collection library, so we decided to provide our own reactive interface with cancellation support to minimize extra dependencies.

As we saw in the introduction, reactive programming is also useful for writing user interfaces (UI) with DOM and Scala.js. Airframe Rx was inspired by [monadic-html](https://github.com/OlivierBlanvillain/monadic-html), which also provides `Rx` interface. We extended monadic-html to support error propagation (with `OnError` event) and reporting the end of streams (with `OnCompletion` event) so that we can support gRPC, which needs to terminate RPC connection at some point. With this extension, we can also support combining multiple streams (e.g., concat, join, zip, etc.)
