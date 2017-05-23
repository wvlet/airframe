Surface: A Reflection-Free Object Shape Inspector
===
[![Gitter Chat][gitter-badge]][gitter-link] [![Build Status](https://travis-ci.org/wvlet/surface.svg?branch=master)](https://travis-ci.org/wvlet/surface) [![Coverage Status][coverall-badge-svg]][coverall-link] [![Latest version](https://index.scala-lang.org/wvlet/surface/surface/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/surface) [![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.14.svg)](https://www.scala-js.org)
[![Scaladoc](http://javadoc-badge.appspot.com/org.wvlet/surface_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/org.wvlet/surface_2.12)

[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/wvlet?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge-svg]: https://coveralls.io/repos/github/wvlet/surface/badge.svg?branch=master
[coverall-link]: https://coveralls.io/github/wvlet/surface?branch=master

Surface is a reflection-free object surface inspection library, which is compatible between Scala and Scala.js 

- [Surface Design Document](https://docs.google.com/document/d/1U71rM6KmTaMWRdbA1MNL8MkMPi5ik4AIQyC7Er675-o/edit)

# Overview

Surface provides functionalities to read the ***surface*** of an object, which includes:
- Object parameter names and its types. 
- Object methods defined in an object with method argument names and types.

Surface is useful for:
- Writing object serializer / deserializer without using any boilerplates.
- Automatically generating code based on class parameters, method definitions, etc. 
- Dependency injection based on object shapes (e.g., [Airframe](https://github.com/wvlet/airframe))

# Usage
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/surface_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/surface_2.12/)

**build.sbt**
```scala
libraryDependencies += "org.wvlet" %% "surface" % "(version)"

// For Scala.js
libraryDependencies += "org.wvlet" %%% "surface" % "(version)"
```

## Surface.of[X]

```scala
import wvlet.surface._

case class A(id:Int, name:String)

val surface = Surface.of[A]
println(surface.toString) // This will show A(id:Int, name:String)

// Find object parameters
surface.params.mkString(", ") // Returns "id:Int, name:String"

// Object factory
surface.objectFactory.map{ f =>
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
import wvlet.surface.Surface
import wvlet.surface.tag._

class Fruit
trait Apple
trait Banana

Surface.of[Fruit @@ Apple]
Surface.of[Fruit @@ Banana]
```

### Runtime Annotation

Reading runtime-annotation is supported for JVM projects. Import `wvlet.surface.runtime._` to use this feature.

```scala
import wvlet.surface.runtime._
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

# Discussion

## Why not using Scala reflection?
Because runtime reflection is slow and postpones detecting problems related to object inspection at run-time. Surface reports any failure in object inspection at compile time. To reduce the performance overhead, Surface uses [Scala Macros](http://docs.scala-lang.org/overviews/macros/overview.html) (planning to migrate [scala.meta](http://scalameta.org/) in future) to perform object inspection at compile time. 

