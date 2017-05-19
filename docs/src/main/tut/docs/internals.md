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
  .newSesion // Session holds the avove instance of B
 
val app = session.build[App]
```

### Injecting Session

To create an instance of A and B, we need to pass the instance of Session while building objects because the session has an instance of B.
But trait `App` nor `A` doesn't know anything about the session.

How do we pass a reference to the Session?

A trick is inside `build[App]`, `bind[A]`, and `bind[B]`.

Let's look at how `session.build[App]` will work when creating an instance of `App`.
Airframe expands this code as follows at compile-time:

```scala
// val app = session.build[App] (original code)
val app: App = {
  // Extends SessionHolder to pass Session object
  new App extends SessionHolder {
    // Inject a reference to the current session
    def __current_session = session

    // val a = bind[A] (original code)
    // If type A is instantiatable trait (non abstract type)
    val a: A = {
      // Trying to find a session (through SessionHolder trait).
      // If no session is found, MISSING_SESSION exception will be thrown
      val session = wvlet.airframe.Session.findSession(this)
      val binder: Session => A = (sesssion: Session =>
        // Register a code for instantiating A 
        session.getOrElseUpdate(Surface[A],
	  (new A with SessionHolder { def __current_session = session }).asInstanceOf[A]
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

In the above macro-expanded code, `A` will be instantiated with SessionHolder trait, which has `__current_session` definition.

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

## Instantiation Methods

When `bind[X]` is used, according to the type of `X` different codes will be generated:

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
session.get(Surface[X])
```




## Suface

Airframe uses `Surface[X]` as identifiers of object types. [Surface](https://github.com/wvlet/surface) is a reflection-free object type inspection library. 

Here are some exmaples of Surface.
```scala
Surface.of[A] // A
Surface.of[Seq[Int]] // Seq[Int]
Surface.of[Seq[_]] // Seq[_]
// Seq[Int] and Seq[_] are different types as Surface

// Type alias
type MyInt = Int
Surface.of[MyInt] // MyInt:=Int
```

Scala is a JVM language, and at the byte-code level all of generics type parameters will be removed (type erasure).
That means, we cannot distinguish between `Seq[Int]` and `Seq[_]` within the byte code because these types will be the same type `Seq[AnyRef]`:
```
Seq[Int] => Seq[AnyRef]
Seq[_] => Seq[AnyRef]
```

Similarly, type alias `MyInt` and `Int` will be the same at the byte-code level.

Scala has runtime-reflection, which can read ScalaSig, detailed type information embedded in class files (byte codes), so
by reading ScalaSig at runtime we can resolve actual generic types, aliases, etc.

However runtime-reflection is slow and not always available. For example [Scala.js](https://www.scala-js.org/) doesn't support runtime reflections.
So we cannot use ScalaSig for resolving type information.

That is why we are using [Surface](https://github.com/wvlet/surface), a macro-based type information extractor, which works both in Scala and Scala.js.


### Surface Parameters

Surface also holds object parmeters, so that we can find objects necessary for building `A`:
```scala
case class A(b:B, c:C)

// B and C will be necessary to build A
Surface.of[A] => Surface("A", params:Seq("b" -> Surface.of[B], "c" -> Surface.of[C]))
```
