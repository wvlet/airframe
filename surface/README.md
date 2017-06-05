Surface: A Reflection-Free Object Shape Inspector
===
[![Gitter Chat][gitter-badge]][gitter-link] [![Build Status](https://travis-ci.org/wvlet/surface.svg?branch=master)](https://travis-ci.org/wvlet/surface) [![Coverage Status][coverall-badge-svg]][coverall-link] [![Latest version](https://index.scala-lang.org/wvlet/surface/surface/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/surface) [![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.14.svg)](https://www.scala-js.org)
[![Scaladoc](http://javadoc-badge.appspot.com/org.wvlet/surface_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/org.wvlet/surface_2.12)

[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/wvlet?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge-svg]: https://coveralls.io/repos/github/wvlet/surface/badge.svg?branch=master
[coverall-link]: https://coveralls.io/github/wvlet/surface?branch=master

Surface is an object surface inspection library, which is compatible between Scala and Scala.js 

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

## surface.of[X]

```scala
import wvlet.surface

case class A(id:Int, name:String)

val surface = surface.of[A]
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

surface.of[UserName] //  Returns UserName:=String

```

### Tagged Type

To have different surfaces for the same type, you can use tagged type (@@):

```scala
import wvlet.surface.surface
import wvlet.surface.tag._

class Fruit
trait Apple
trait Banana

surface.of[Fruit @@ Apple]
surface.of[Fruit @@ Banana]
```

### Runtime Annotation

Reading runtime-annotation is supported for JVM projects. Import `wvlet.surface.reflect._` to use this feature.

```scala
import wvlet.surface
import wvlet.surface.reflect._
import javax.annotation.Resource
 
@Resource(name="my resource")
class A(@Resource(name = "param 1") a:String)

val s = surface.of[A]
// Reading class annotation
val a = s.findAnnotationOf[Resource]
a.get.name // "my resource"

// Reading parameter annotation
val r = s.params.find(_.name == "a").findAnnotationOf[Resource]
r.get.name // "param 1"

```
