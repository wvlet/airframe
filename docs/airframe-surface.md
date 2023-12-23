---
id: airframe-surface
title: airframe-surface: Object Shape Inspector
---

airframe-surface enables reading type information, such as:
- Constructor parameter names and their types
- Methods defined in an object with method argument names and types.

## Usage 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-surface_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-surface_2.12/)
[![Scaladoc](http://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-surface_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-surface_2.12)

**build.sbt**
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-surface" % "(version)"

// For Scala.js
libraryDependencies += "org.wvlet.airframe" %%% "airframe-surface" % "(version)"
```

- `Surface.of[X]` returns parameters, 
- `MethodSurface.of[X]` returns the list of methods in `X` and their parameter names and types. 
- airframe-surface is compatible between Scala and Scala.js 


Applications of Surface include:
- Writing object serializer / deserializer without any boilerplates. 
  - For example, [airframe-codec](https://wvlet.org/airframe/docs/airframe-codec.html) Automatically generates code serialiation and deserizliation using Surface.
- Dependency injection based on object shapes (e.g., [Airframe DI](https://wvlet.org/airframe/docs/airframe.html))
  - airframe-di detects dependent types using Surface
- Surface can be an indentifer of object types and type aliases.


Initial design note: [Surface Design Document](https://docs.google.com/document/d/1U71rM6KmTaMWRdbA1MNL8MkMPi5ik4AIQyC7Er675-o/edit)


## Surface.of[X]

```scala
import wvlet.airframe.surface.Surface

case class A(id:Int, name:String)

val s = Surface.of[A]
println(s.toString) // This will show A(id:Int, name:String)

// Find object parameters
s.params.mkString(", ") // Returns "id:Int, name:String"

// Object factory
s.objectFactory.map{ f =>
  f.newInstance(Seq(1, "leo"))
}
// Some(A(1, "leo"))

```

## Surface.methodsOf[X]

Extract the public method definitions of a given class:

```scala
class A {
  def hello(name:String): String = s"hello ${name}"
}

val methods = Surface.methodsOf[A]
val h = methods.head
h.name // "hello"
h.returnType // Surface.of[String]
h.args(0).surface // Surface.of[String]
```

### Type alias

```scala
type UserName = String

Surface.of[UserName] //  Returns UserName:=String
```

In Scala 3, [opaque type aliases](https://docs.scala-lang.org/scala3/reference/other-new-features/opaques.html) are also supported:

```scala
opaque type UserName = String
Surface.of[UserName] // Returns UserName:=String
```

Warning: In Scala 3, due to the eager type alias expansion https://github.com/wvlet/airframe/issues/2200, the surface of a type alias can be the same surface of the referenced type. If you need to differentiate the same type with different names, use intersection types.

```scala
type UserName = String

// In Scala 3, this function can be resolved as String => Int:
val f1 = { (x: UserName) => x.toString.length }

// To resolve this as UserName => Int, explicitly specify the function type:
val f2: Funciton1[UserName, Int] = { (x: UserName) => x.toString.length }
```

### Intersection Types

In Scala3, you can use [intersection types](https://docs.scala-lang.org/scala3/reference/new-types/intersection-types.html) for labeling types.

For example, you can use `String & Environment` and `String & Stage` for differentiating the same type for different purposes:

```scala
import wvlet.airframe.surface.Surface

trait Environment
trait Stage

Surface.of[String & Environment]
Surface.of[String & Stage]
```

Union type `A | B` is also supported:
```scala
import wvlet.airframe.surface.Surface

Surface.of[String | Int]
```

### Tagged Types

To have different surfaces for the same type, you can use tagged type (@@):

```scala
import wvlet.airframe.surface.Surface
import wvlet.airframe.surface.tag._

class Fruit
trait Apple
trait Banana

Surface.of[Fruit @@ Apple]
Surface.of[Fruit @@ Banana]
```

### Runtime Annotation

Reading runtime-annotation is supported for JVM projects. Import `wvlet.airframe.surface.reflect._` to use this feature.

```scala
import wvlet.airframe.surface.Surface
import wvlet.airframe.surface.reflect._
import javax.annotation.Resource
 
@Resource(name="my resource")
class A(@Resource(name = "param 1") a:String)

val s = Surface.of[A]
// Reading class annotation
val a = s.findAnnotationOf[Resource]
a.get.name // "my resource"

// Reading parameter annotation
val r = s.params.find(_.name == "a").findAnnotationOf[Resource]
r.get.name // "param 1"

```
