---
layout: docs
title: Bindings
---

# Advanced Bindings

Here are examples of bindings using type parameters. In Airframe,

```scala
bind[Seq[_]]
bind[Seq[Int]]
bind[Seq[String]]


bind[Map[Int,String]]
bind[Map[_,_]]
```

Behind the scene, Airframe uses [Surface](https://github.com/wvlet/surface/) as identifier of class types.

## Type alias binding

If you need to bind the same type objects in a different manner, you can use type alias of Scala.
For example,
```scala
case class Fruit(name: String)

type Apple = Fruit
type Banana = Fruit

trait TaggedBinding {
  val apple  = bind[Apple]
  val banana = bind[Banana]
}
 ```

Alias binding is also useful to inject primitive type values:
```scala
type Env = String

trait MyService {
  // Coditional binding
  lazy val threadManager = bind[Env] match {
     case "test" => bind[TestingThreadManager] // prepare a testing thread manager
     case "production" => bind[ThreadManager] // prepare a thread manager for production
  }
}

val coreDesign = newDesign
val testingDesign = coreDesign.bind[Env].toInstance("test")
val productionDesign = coreDesign.bind[Env].toInstance("production")
```

## Tagged type binding

Taggged binding `@@` is also useful to annotate type names:

```scala
// This import statement is necessary to use tagged type (@@)
import wvlet.surface.tag._

trait Name
trait Id

trait A {
  val name = bind[String @@ Name]
  val id = bind[Int @@ Id]
}
```

## Reuse bindings with mixin

To reuse bindings, we can create XXXService traits and mix-in them to build a complex object.

```scala
import wvlet.airframe._

trait PrinterService {
  val printer = bind[Printer] // It can bind any Printer types
}

trait FortuneService {
  val fortune = bind[Fortune]
}

trait FortunePrinterMixin extends PrinterService with FortuneService {
  printer.print(fortune.generate)
}
```

### Override bindings

It is also possible to manually inject an instance implementation. This is useful for changing the behavior of objects for testing:
```scala
trait CustomPrinterMixin extends FortunePrinterMixin {
  override val printer = new Printer { def print(s:String) = { Console.err.println(s) } } // Manually inject an instance
}
```
