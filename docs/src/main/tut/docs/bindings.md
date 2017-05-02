---
layout: docs
title: Binding types
---

## Local variable binding

Using local variables is the simplest way to binding objects:

```scala
trait FortunePrinterEmbedded {
  val printer = bind[Printer]
  val fortune = bind[Fortune]

  printer.print(fortune.generate)
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

It is also possible to manually inject an instance implementation. This is useful for changing the behavior of objects for testing:
```scala
trait CustomPrinterMixin extends FortunePrinterMixin {
  override val printer = new Printer { def print(s:String) = { Console.err.println(s) } } // Manually inject an instance
}
```

## Type alias binding

Airframe can provide separate implementations to the same type object by using type alias:
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
