---
id: airframe-rx
title: airframe-rx: ReactiveX interface
---

airframe-rx is a [ReactiveX](http://reactivex.io/) implementation for Scala and Scala.js, which is used for:
- Streaming support of [Airframe gRPC](airframe-grpc.md)
- [Interactive rendering of DOM objects](https://github.com/wvlet/airframe/blob/master/airframe-http-rx/.js/src/main/scala/wvlet/airframe/http/rx/html/DOMRenderer.scala) (airframe-http-rx)


## Why Reactive Programming?

Reactive programming is a model of event-based processing. For example, if you want to keep monitoring an event and write a chain of actions after observing some changes of the event, reactive programming is the right choice for you. 

For example, when writing an Web UI, we usually need to change DOM elements based on the state of some variable. This example will update the contents of DOM if `counter` variable is updated: 

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

`Rx[A]` represents a reactive component which will be updated if there is any change in its upstream operators. In this example, we are chaining an action (map operator) based on the current state of `counter` variable. These actions are observing the state of the variable, and if the `counter` variable is updated, the registered actions will be triggered. 

As an example of server-side programming, [gRPC](https://grpc.io/), which supports client/server-side streaming as well as bi-directional streaming, is a good example of reactive programming. By using [Airframe RPC](airframe-rpc.md), you can create a gRPC server, which returns multiple String messages as a stream:
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

The above two examples show non-blocking code, which means that the subsequent code can be processed while we are waiting for updates of the observed events. If you use reactive components like `Rx[A]`, writing interactive UIs or RPC clients and servers becomes much easier than managing threads and event update signals by yourself. 

For interested readers, visit [ReactiveX](http://reactivex.io/) page. You can find various types of event-based processing patterns. You will notice that the most of these operators are quite similar to [Scala collection library operators](https://docs.scala-lang.org/overviews/collections/trait-traversable.html), such as map, flatMap, filter, zip, etc. If you are alrady familiar to Scala, learning reactive programming is not so difficult. 


## Why Airframe Rx?


In Scala, _Future[A]_ can be used for writing asynchronous and reactive programming. `Future[A]` represents a value of type _A_ that will be available in future and encapsulates operators for producing the value. We can use Future with [scala.concurrent.Future](https://docs.scala-lang.org/overviews/core/futures.html) or [Twitter's Future](https://twitter.github.io/finagle/guide/Futures.html), etc. Future is good for writing asynchronous data processing. Users can concatenate a sequence of operators for processing data without blocking the code execution. So, while waiting the data from remote sources, other tasks can be processed in parallel.

One of the drawbacks of _Future_ is that cancelling already started tasks is not straightforward. [Monix](https://monix.io) has implemented a cancelable task abstraction, denoted _Task[A]_, which is similar to Future, but isolates operators and their executions so that we can have more control over the stream processing flows. 

Two essential parts of reactive programming are: 
- Isolating operators and their execution
- Cancelable tasks

While Monix is targeting for high-performance asynchronous event-based programming, reactive programming is also useful for writing user interfaces (UI) with DOM and Scala.js. This Rx implementation was originally created by following monadic-html https://github.com/OlivierBlanvillain/monadic-html, but this has no error handling and error propagation to the upstream operators, so reporting OnError(exception) or OnCompletion event was difficult in monadic-html. Knowing the end of streams is important to support gRPC, which needs to terminate RPC connection at some point, and also for combining multiple streams (e.g., concat, join, zip, etc.)

Rx[A] is a chain of operators that will be applied to the stream of objects A and it's used for dynamically updating a part of DOM elements in airframe-http-rx. See DOMRenderer code for more details.


With this PR, adding more ReactiveX methods will also be possible (e.g., Rx.buffer, Rx.retry, etc.). And Rx operator case classes are independent from RxRunner, so it is also possible to add different RxRunners for supporting concurrent programming if we want.

For our use cases, we only need reactive DOM rendering (RxElement) and gRPC streaming. After merging this PR, I'd like to extract this code as airframe-rx module under wvlet.airframe.rx package to maintain this code as a thin reactive interface with less dependencies.



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

// Call cancel to stop the suscription
c.cancel
```

