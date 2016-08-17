# airframe  [![Gitter Chat][gitter-badge]][gitter-link] [![CircleCI][circleci-badge]][circleci-link] [![Coverage Status][coverall-badge]][coverall-link]
Dependency injection library tailored to Scala.

[circleci-badge]: https://circleci.com/gh/wvlet/airframe.svg?style=svg
[circleci-link]: https://circleci.com/gh/wvlet/airframe
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/wvlet?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge]: https://coveralls.io/repos/github/wvlet/airframe/badge.svg?branch=master
[coverall-link]: https://coveralls.io/github/wvlet/airframe?branch=master


# Introduction

Airframe injects object dependencies as in [Google Guice](https://github.com/google/guice). 

With Airframe you can build objects in three steps:
- *Bind*: Describe instance types necessary in your class with `bind[X]`: 
```scala
import wvlet.airframe._

trait App {
  val x = bind[X]
  val y = bind[Y]
  // Do something with X and Y
}
```
- *Design*: Describe how to provide object instances:
```scala
val design : Design = 
   Airframe.newDesign
     .bind[X].toInstance(new X)  // Bind type X to a concrete instance
     .bind[Y].toSingleton        // Bind type Y to a singleton object
```
- *Build*: Create a concrete instance:
```scala
val app : App = design.build[App]
```

Airframe creates an `App` instance by searching the design for binding rules of X and Y. 
`Design` class is *immutable*, so you can safely reuse and extend it for creating new types of objects.

The major advantages of Airframe are:
- Simple to use. Just import `wvlet.airframe._` and do the above three steps. 
- You can describe the knowledge on how to create objects within `Design`.
  - It enables you to reuse the same design to prepare objects both in production and test code. This avoids code duplications that create instances with constructors (e.g., `new App(new X, new Y, ...)`).
  - When writing application codes, you only need to care about how to ***use*** objects, rather than how to ***provide*** them. 
- You can mix-in Scala traits that have multiple dependencies, instead of writing constructors that have many arguments.
  - No longer need to remember the constructor argument orders.
  - You can enjoy the flexibility of Scala traits and dependency injection (DI) at the same time.

# Usage

**build.sbt** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/airframe_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/airframe_2.11)
```
libraryDependencies += "org.wvlet" %% "airframe" % "(version)"
```


(The whole code used in this section can be found here [AirframeTest](https://github.com/wvlet/airframe/blob/master/src/test/scala/wvlet/airframe/AirframeTest.scala))

You can inject an object with `bind` method in Airframe. Assume that we want to create a service that prints a greeting at random:

```scala
import wvlet.airframe._ 
import wvlet.log.LogSupport

trait Printer {
  def print(s: String): Unit
}

// Concrete classes which will be bound to Printer
class ConsolePrinter(config: ConsoleConfig) extends Printer { 
  def print(s: String) { println(s) }
}
class LogPrinter extends Printer with LogSupport { 
  def print(s: String) { info(s) }
}

class Fortune { 
  def generate: String = { /** generate random fortune message **/ }
}
```

## Local variable binding

Using local variables is the simplest way to binding objects:

```scala
trait FortunePrinterEmbedded {
  protected val printer = bind[Printer]
  protected val fortune = bind[Fortune]

  printer.print(fortune.generate)
}
```

## Reuse bindings with mixin

To reuse bindings, we can create XXXService traits and mix-in them to build a complex object. 

```scala
import wvlet.airframe._

trait PrinterService {
  protected def printer = bind[Printer] // It can bind any Printer types
}

trait FortuneService {
  protected def fortune = bind[Fortune]
}

trait FortunePrinterMixin extends PrinterService with FortuneService {
  printer.print(fortune.generate)
}
```

It is also possible to manually inject an instance implementation. This is useful for changing the behavior of objects for testing: 
```scala
trait CustomPrinterMixin extends FortunePrinterMixin {
  override protected def printer = new Printer { def print(s:String) = { Console.err.println(s) } } // Manually inject an instance
}
```

## Tagged binding

Airframe can provide separate implementations to the same type object by using object tagging (@@):
```scala
import wvlet.obj.tag.@@
case class Fruit(name: String)

trait Apple
trait Banana

trait TaggedBinding {
  val apple  = bind[Fruit @@ Apple]
  val banana = bind[Fruit @@ Banana]
}
 ```

## Object Injection

Before binding objects, you need to define a `Design` of dependent components. It is similar to `modules` in Guice.

```scala
val design = Airframe.newDesign
  .bind[Printer].to[ConsolePrinter]  // Airframe will generate an instance of ConsolePrinter by resolving its dependencies
  .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err)) // Binding an actual instance
```

You can also define bindings to the tagged objects:

```scala
val design = Airframe.newDesign
  .bind[Fruit @@ Apple].toInstance(Fruit("apple"))
  .bind[Fruit @@ Banana].toInstance(Fruit("banana"))
  .bind[Fruit @@ Lemon].toInstance(Fruit("lemon"))
````

To bind a class to a singleton, use `toSingleton`:

```scala
class HeavyObject extends LogSupport { /** */ }

val design = Airframe.newDesign
  .bind[HeavyOBject].toSingleton
````

We can create an object from a design by using `build`:

```
design.build[FortunePrinterMixin]
```

See more detail in [AirframeTest](https://github.com/wvlet/airframe/blob/master/src/test/scala/wvlet/airframe/AirframeTest.scala).

# LICENSE

[Apache v2](https://github.com/wvlet/airframe/blob/master/LICENSE)
