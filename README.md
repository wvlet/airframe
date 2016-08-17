# airframe  [![Gitter Chat][gitter-badge]][gitter-link] [![CircleCI][circleci-badge]][circleci-link] [![Coverage Status][coverall-badge]][coverall-link]
Dependency injection library tailored to Scala

[circleci-badge]: https://circleci.com/gh/wvlet/airframe.svg?style=svg
[circleci-link]: https://circleci.com/gh/wvlet/airframe
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/wvlet?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge]: https://coveralls.io/repos/github/wvlet/airframe/badge.svg?branch=master
[coverall-link]: https://coveralls.io/github/wvlet/airframe?branch=master

# Usage

Airframe inject dependencies based on trait as interface in Guice. The whole code is written in [AirframeTest](https://github.com/wvlet/airframe/blob/master/src/test/scala/wvlet/airframe/AirframeTest.scala).
 
You can inject a object with `bind` method in Airframe. Assume we want to create a service which prints a greeting at random.

```scala
trait Printer {
  def print(s: String): Unit
}

// Concrete classes which will be bind to Printer
class ConsolePrinter(config: ConsoleConfig) extends Printer with LogSupport { /** */ }
class LogPrinter extends Printer with LogSupport { /** */ }

class Fortune { 
  def generate: String = { /** */ }
}
```

## Mix-in instances

The simple way is to create a service trait which uses binding objects. Since trait can be shared multiple components and a class 
can mix-in any traits, this is a simple way to use binding objects.

```scala
trait PrinterService {
  protected def printer = bind[Printer] // It's binded any Printer mix in instances.
}

trait FortuneService {
  protected def fortune = bind[Fortune]
}

trait FortunePrinterMixin extends PrinterService with FortuneService {
  printer.print(fortune.generate)
}
```

## Local variable binding

We can bind a object to local variable explicitly.

```scala
trait FortunePrinterEmbedded {
  protected def printer = bind[Printer]
  protected def fortune = bind[Fortune]
  
  printer.print(fortune.generate)
}
```

## Tagged binding

Airframe enables us to bind multiple implementation to a trait by using object tagging.
 
 
 ```scala
 import wvlet.obj.tag.@@
 case class Fruit(name: String)
 
 trait Apple
 trait Banana
 trait Lemon

 trait TaggedBinding {
   val apple  = bind[Fruit @@ Apple]
   val banana = bind[Fruit @@ Banana]
   val lemon  = bind(lemonProvider _)
 
   def lemonProvider(f: Fruit @@ Lemon) = f
 }
 ```


## Injecting

It is necessary to define `Design` of dependency components before using binding objects. It's similar to `module` in Guice.

```scala
val design = Airframe.newDesign
  .bind[Printer].to[ConsolePrinter]  // Generated in resolved dependency components in Airframe design
  .bind[ConsoleConfig].toInstance(ConsoleConfig(System.err)) // Binding actual instance
```

Binding tagged object can be done with `@@`.

```scala
val design = Airframe.newDesign
  .bind[Fruit @@ Apple].toInstance[Fruit("apple")]
  .bind[Fruit @@ Banana].toInstance[Fruit("banana")]
  .bind[Fruit @@ Lemon].toInstance[Fruit("lemon")]
````

If we want to bind a class as singleton, `toSingleton` cab be used.

```scala
class HeavyObject extends LogSupport { /** */ }

val design = Airframe.newDesign
  .bind[HeavyOBject].toSingleton
````

We can create a object needed with `build` keyword.

```
design.build[FortunePrinterMixin]
```

See more detail in [AirframeTest](https://github.com/wvlet/airframe/blob/master/src/test/scala/wvlet/airframe/AirframeTest.scala).

# LICENSE

[Apache v2](https://github.com/wvlet/airframe/blob/master/LICENSE)