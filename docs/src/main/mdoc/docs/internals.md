---
layout: docs
title: Airframe Internals
---

# Airframe Internals

This page describes the internals of Airframe for developers who are interested in extending Airframe.

## Session 

A Session in Airframe is a holder of instances and binding rules. Airframe is designed to simplify the instantiation of complex objects like:
```scala
new App(a = new A(b = new B), ...)
```

into this form:
```scala
session.build[App]
```

In this code Airframe DI will take care of the object instantiation by automatically finding how to build `App`, and its dependencies `A`, `B`, etc.

### Example

To explain the role of Session, let's start with a simple code that uses Airframe bindings:

```scala
import wvlet.airframe._

trait App {
  val a = bind[A]
}

trait A {
  val b = bind[B]
}

val session =
  newDesign
  .bind[B].toInstance(new B(...))
  .newSesion // Creates a session thats holds the above instance of B
 
val app = session.build[App]
```
This code builds an instance of `App` using a concrete instance of `B` stored in the session.

### Injecting Session

To create instances of `A` and `B` inside `App`, we need to pass the concrete instance of B though the session instance. But trait definitions of `App` and `A` don't know anything about the session, so we need a way to resolve the instance of `B`.

To do so, Airframe will pass a reference to the Session while building `App`, `A`, and `B`. A trick is inside the implementation of `build` and `bind`. Let's look at how `session.build[App]` will work when creating an instance of `App`.

Here is the code for building an App:

```scala
val app = session.build[App]
```

Airframe expands this code into this form at compile-time:

```scala
val app: App = 
{ ss: Session =>
  // Extends DISupport to pass Session object
  new App extends DISupport {
    // Inject a reference to the current session
    def session = ss

    // val a = bind[A] (original code inside App)
    // If type A is instantiatable trait (non abstract type)
    val a: A = {
      // Trying to find a session (using DISupport.session).
      // If no session is found, MISSING_SESSION exception will be thrown
      val ss1 = wvlet.airframe.Session.findSession(this)
      val binder: Session => A = (ss2: Session =>
        // Register a code for instantiating A 
        ss2.getOrElseUpdate(Surface.of[A],
	  (new A with DISupport { def session = ss1 }).asInstanceOf[A]
        )
      )
      // Create an instance of A by injecting the current session
      binder(ss1)
    }
  }
}.apply(session)
```

To generate the above code, Airframe is using [Scala Macros](http://docs.scala-lang.org/overviews/macros/overview.html). You can find the actual macro definitions in [AirframeMacros.scala](https://github.com/wvlet/airframe/blob/master/airframe-macros/shared/src/main/scala/wvlet/airframe/AirframeMacros.scala)

When `bind[X]` is called, the active session must be found. So if you try to instantiate A without using `session.build[A]`, `MISSING_SESSION` runtime-error will be thrown:

```scala
val a1 = new A // MISSING_SESSION error will be thrown at run-time

val a2 = session.build[A] // This is OK
```

In the above code, `A` will be instantiated with DISupport trait, which has `session` definition. `bind[B]` inside trait `A` will be expanded liks this similarly:

```scala
new A extends DISupport {
  // (original code) val b = bind[B]
  val b: B = { ss: Session =>
    val ss = findSession(this)
    // If the session already has an instance of B, return it. Otherwise, craete a new instance of B 
    ss.getOrElse(Surface.of[B], (session:Session => new B with DISupport { ... } ))
  }
  // Inject the current session to build B
  .apply(session) 
}
```

### Comparison with a naive approach

The above macro-generated code looks quite scarly at first glance. 
However, if you write similar code by yourself, you will end up doing almost the same thing with Session.

For example, consider building `App` trait using a custom `B` instance:

```scala
{ 
  val myB = new B {}
  val myA = new A(b = myB) {}
  new App(a = myA)
}
// How can we find myA and myB after exiting the scope?
// What if a and b hold resources (e.g., network connection, database connection, etc.), that need to be released later?
```

To manage life cycle of A and B, you eventually needs to store the object references somewhere like this:

```scala
// Assume storing objects in a Map-backed session
val session = Map[Class[_], AnyRef]()

session += classOf[B] -> new B {}
session += classOf[A] -> new A(b=session.get(classOf[B])) {}

val app = new App(a = session.get(classOf[A])) {}
session += classOf[App] -> app

// At shutdown phase
session.objects.foreach { x=> 
  x match {
    case a:A => // release A
    case b:B => // release B ...
    case _ => ...
  }
}

```
As we have seen in the example of [Service Mix-in](use-cases.html#service-mix-in), if we need to manage hundreds of services,
manually writing such object management functions will be cumbersome. Airframe helps you to oraganize building service objects. 


## Instantiation Methods

When `bind[X]` is used, according to the type of `X` different code can be generated:

- If `X` is a non-abstract trait, the generated code will be like the above.
- If `X` is a non-abstract class that has a primary constructor, Airframe inject dependencies to the constructor arguments: 

```scala
// case class X(a:A, b:B, ..)

val surface = Surface.of[X]
// build instances of a, b, ...
val args = surface.params.map(p -> session.getInstance(p.surface))
surface.objectFactory.newInstance(p)
```

- If `X` is an abstract class or trait, `X` needs to be found in X because `X` cannot be instantiated automatically:

```scala
session.get(Surface.of[X])
```


## Suface

Airframe uses `Surface.of[X]` as identifiers of object types. [Surface](https://github.com/wvlet/airframe/tree/master/surface) is an object type inspection library.

Here are some examples of Surface:
```scala
import wvlet.surface

Surface.of[A] // A
Surface.of[Seq[Int]] // Seq[Int]
Surface.of[Seq[_]] // Seq[_]
// Seq[Int] and Seq[_] are different types as Surface

// Type alias
type MyInt = Int
Surface.of[MyInt] // MyInt:=Int
```

Surface treats type aliases (e.g., MyInt) and Int as different types. This provides flexibilities in binding different objects to the same type. For example, you can define MyInt1, MyInt2, ... Google Guice doesn's support this kind of bindings to the same types.

Scala is a JVM language, so at the byte-code level, all of generics type parameters will be removed because of type erasure.
That means, we cannot distinguish between `Seq[Int]` and `Seq[_]` within the byte code; These types are the same type `Seq[AnyRef]` in the byte code:
```
Seq[Int] => Seq[AnyRef]
Seq[_] => Seq[AnyRef]
```
Surface knows the detailed type parameters like `Seq[Int]` and `Seq[_]`, so it can distinguish these two `Seq` types.


To provide detailed type information only available at compile-time, Surface uses runtime-reflecation, which can pass compile-type type information such as 
 function argument names, generic types, etc., to the runtime environment. Surface extensively uses `scala.reflect.runtime.universe.Type` 
information so that bindings using type names can be convenient for the users.  

For compatibility with [Scala.js](https://www.scala-js.org/), which doesn't support any runtime reflection,
Surface uses Scala macros to embed compile-time type information into the runtime objects.

### Surface Parameters

Surface also holds object parameters, so that we can find objects necessary for building `A`:
```scala
case class A(b:B, c:C)

// B and C will be necessary to build A
Surface.of[A] => Surface("A", params:Seq("b" -> Surface.of[B], "c" -> Surface.of[C]))
```
