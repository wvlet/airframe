---
layout: docs
title: Advanced Binding Types
---

# Advanced Binding Types

## Generic Type Binding

Airframe can bind objects to generics types. Traditional DI libraries for Java (e.g., [Guice](https://github.com/google/guice), etc.) cannot
distinguish generic classes that have different type parameters (e.g., `Seq[Int]`, `Seq[String]`) because Java compiler applies [type erasure](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html), and converts them to the same `Seq[Object]` type. In Airframe, generic types with different type parameters will be treated differently. For example, all of the following bindings can be assigned to different objects:

```scala
bind[Seq[_]]
bind[Seq[Int]]
bind[Seq[String]]

bind[Map[Int,String]]
bind[Map[_,_]]
```

Behind the scene, Airframe uses [Surface](https://github.com/wvlet/airframe/surface/) as identifier of types so that we can extract these types identifiers at compile time.

## Type Alias Binding

If you need to bind different objects to the same data type, use type aliases of Scala. For example,
```scala
case class Fruit(name: String)

type Apple = Fruit
type Banana = Fruit

trait TaggedBinding {
  val apple  = bind[Apple]
  val banana = bind[Banana]
}
 ```

Alias binding is useful to inject primitive type values:
```scala
import wvlet.airframe._

type Env = String

trait MyService {
  // Conditional binding
  lazy val threadManager = bind[Env] match {
     case "test" => bind[TestingThreadManager] // prepare a testing thread manager
     case "production" => bind[ThreadManager] // prepare a thread manager for production
  }
}

val coreDesign = newDesign

val testingDesign =
  coreDesign.
    bind[Env].toInstance("test")

val productionDesign =
  coreDesign
    .bind[Env].toInstance("production")
```

## Multi-Binding

If you want to switch a service to be called depending on the user input, you can just use Scala's functionality + Airframe binding. 

To illustrate this, consider building an web application that receives a request and returns a string message.
`Dispatcher` class receives an URL path and choose an appropriate `Handler` to use:

```scala
import wvlet.airframe._

trait Handler {
  def handle(request:Request): String
}

trait DefaultHandler extends Handler {
  def handle(request:Request): String = "hello"
}

trait InfoHandler extends Handler {
  def handle(rquest:Request): String = "info"
}

trait Dispatcher {
  private val dispatcher: String => Handler = {
    case "info" => bind[InfoHandler]
    case _ => bind[DefaultHandler]
  }
  
  def dispatch(path:String, request:Request): String =  {
     dispatcher(path).handle(request)
  }
}
```

In Google Guice, we need to use a special binder like [Multibinder](https://github.com/google/guice/wiki/Multibindings). 
In Airframe, we just need to write a Scala code that uses `bind[X]`. 

## Tagged Type Binding

Tagged binding `@@` is also useful to annotate type names:

```scala
// This import statement is necessary to use tagged type (@@)
import wvlet.airframe.surface.tag._

trait Name
trait Id

trait A {
  val name = bind[String @@ Name]
  val id = bind[Int @@ Id]
}
```
