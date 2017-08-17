---
layout: docs
title: Airframe Internals
---

# Airframe Internals

This page describes the internals of Airframe for developers who are interested in the internals of Airframe.

## Session 

Session in Airframe is a holder of instances and binding rules. Airframe is designed to simplify complex object instantiation like:
```scala
new App(a = new A(b = new B), ...)
```

into this form:
```scala
session.build[App]
```

so that Airframe (DI framework) can take care of object instantiation by automatically finding how to build `App`, `A`, `B`, etc.

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
  .newSesion // Session holds the above instance of B
 
val app = session.build[App]
```
This code builds an instance of `App` using a concrete instance of `B` stored in Session.

### Injecting Session

To create an instance of `A` and `B` inside `App`, we need to pass the instance of Session while building objects because the session holds an instance of B.
But trait `App` nor `A` doesn't know anything about the session.

How do we pass a reference to the Session? A trick is inside `build` and `bind`.

Let's look at how `session.build[App]` will work when creating an instance of `App`.
Airframe expands this code as follows at compile-time:

```scala
// val app = session.build[App] (original code)
val app: App = {
  // Extends SessionHolder to pass Session object
  new App extends SessionHolder {
    // Inject a reference to the current session
    def airframeSession = session

    // val a = bind[A] (original code)
    // If type A is instantiatable trait (non abstract type)
    val a: A = {
      // Trying to find a session (through SessionHolder trait).
      // If no session is found, MISSING_SESSION exception will be thrown
      val session = wvlet.airframe.Session.findSession(this)
      val binder: Session => A = (sesssion: Session =>
        // Register a code for instantiating A 
        session.getOrElseUpdate(Surface[A],
	  (new A with SessionHolder { def airframeSession = session }).asInstanceOf[A]
        )
      )
      // Create an instance of A by injecting the current session
      binder(session)
    }
  }
}
```

To generate the above code, Airframe is using [Scala Macros](http://docs.scala-lang.org/overviews/macros/overview.html). You can find the actual macro definitions in [AirframeMacros.scala](https://github.com/wvlet/airframe/blob/master/airframe-macros/shared/src/main/scala/wvlet/airframe/AirframeMacros.scala)

The active session should be found when `bind[X]` is called. So if you try to instantiate A without using `session.build[A]`, `MISSING_SESSION` runtime-error will be thrown:
```
val a1 = new A // MISSING_SESSION error will be thrown at run-time

val a2 = session.build[A] // This is OK
```

In the above macro-expanded code, `A` will be instantiated with SessionHolder trait, which has `airframeSession` definition.

So `bind[B]` inside trait `A` will be expanded similarry:
```scala
new A extends SessionHolder {
  // (original code) val b = bind[B]
  val b: B = {
    val session = findSession(this)
    val binder = session..getOrElseUpdate(Surface[B], (session:Session => new B with SessionHolder { ... } ))
    binder(session)
  }
}
```

### Comparsion with a naive approach

At first look, the above macro expantion looks quite scarly, however, when calling constructor of `App` you are actually doing similar things:
```
{ 
  val myB = new B {}
  val myA = new A(b = myB) {}
  new App(a = myA)
}
// How can we find myA and myB after exiting the scope?
// What if a and b hold resources (e.g., network connection, database connection, etc.), that need to be released later?
```

To manage life cycle of A and B, you eventually needs to store the object references somewhere:
```
// Assume storing objects in a Map
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
manually writing such object management codes will be cumbersome tasks. 


## Instantiation Methods

When `bind[X]` is used, according to the type of `X` different codes will be generated:

- If `X` is a non-abstract trait, the generated code will be like the above.
- If `X` is a non-abstract class that has a primary constructor, Airframe inject dependencies to the constructor arguments: 

```scala
// case class X(a:A, b:B, ..)

val surface = surface.of[X]
// build instances of a, b, ...
val args = surface.params.map(p -> session.getInstance(p.surface))
surface.objectFactory.newInstance(p)
```

- If `X` is an abstract class or trait, `X` needs to be found in X because `X` cannot be instantiated automatically:

```scala
session.get(surface.of[X])
```


## Suface

Airframe uses `Surface[X]` as identifiers of object types. [Surface](https://github.com/wvlet/airframe/tree/master/surface) is an object type inspection library.

Here are some examples of Surface:
```scala
import wvlet.surface

surface.of[A] // A
surface.of[Seq[Int]] // Seq[Int]
surface.of[Seq[_]] // Seq[_]
// Seq[Int] and Seq[_] are different types as Surface

// Type alias
type MyInt = Int
surface.of[MyInt] // MyInt:=Int
```

Scala is a JVM language so at the byte-code level all of generics type parameters will be removed (type erasure).
That means, we cannot distinguish between `Seq[Int]` and `Seq[_]` within the byte code; These types are the same type `Seq[AnyRef]` in the byte code:
```
Seq[Int] => Seq[AnyRef]
Seq[_] => Seq[AnyRef]
```

Similarly the above type alias `MyInt` and `Int` will be the same types.

To provide detailed type information only avaiable at compile-time, Surface uses runtime-reflecation, which can pass compile-type type information such as 
 function argument names, generic types, etc., to the runtime environment. Surface extensively uses `scala.reflect.runtime.universe.Type` 
information so that bindings using type names can be convenient for the users.  

For compatibility with [Scala.js](https://www.scala-js.org/), which doesn't support any runtime reflection,
Surface uses Scala macros to embed compile-time type information into the runtime objects.

### Surface Parameters

Surface also holds object parmeters, so that we can find objects necessary for building `A`:
```scala
case class A(b:B, c:C)

// B and C will be necessary to build A
surface.of[A] => Surface("A", params:Seq("b" -> surface.of[B], "c" -> surface.of[C]))
```
