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

### Type alias

```scala

type UserName = String

Surface.of[UserName] //  Returns UserName:=String

```

### Tagged Type

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
