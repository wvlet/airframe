AirSpec
======

[AirSpec](https://wvlet.org/airframe/docs/airspec.html) is a functional testing framework for Scala and Scala.js.

# Quick Start

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airspec_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airspec_2.12/)

- AirSpec is available since version 19.8.1

**build.sbt**
```
libraryDependencies += "org.wvlet.airframe" %% "airspec" % "(version)"
testFrameworks += new TestFramework("wvlet.airframe.spec.AirSpecFramework")
```

For Scala.js, use `%%%`:
```
libraryDependencies += "org.wvlet.airframe" %%% "airspec" % "(version)"
```
To run your tests, you only need the following steps:
- Create a class (or object) by extending **`wvlet.airframe.spec.AirSpec`**.
- Define your test cases as ___functions___ (public methods) in this class.
- Use **sbt `> testOnly -- (pattern)`** to run your tests!

![image](https://wvlet.org/airframe/img/airspec/airspec.png)

Tests in AirSpec are designed to be pure Scala as much as possible so as not to introduce any complex DSLs,
which are usually hard to remenber, for writing tests.

Tests in AirSpec are just regular functions in Scala.

Using AirSpec is simple, but it also
supports powerful lifecycle management of objects based on [Airframe DI](https://wvlet.org/airframe/docs/airframe.html).
The function arguments of test methods will be used for passing objects that are necessary for running tests, and
after finishing tests, these objects will be discarded properly.

# Usage

## Writing Unit Tests 

In AirSpec test cases are defined as functions in a class (or an object) extending `AirSpec`.
All public functions (methods) in the class will be executed as test cases:

```scala
import wvlet.airframe.spec._

class MyTest extends AirSpec {
  // Basic assertion
  def emptySeqSizeShouldBe0: Unit = {
    assert(Seq.empty.size == 0)
  }

  // Catch an exception
  def emptySeqHeadShouldFail: Unit = {
    intercept[NoSuchElementException]{
      Seq.empty.head
    }
  }
}
```

AirSpec supports basic assertions like `assert`, `fail`, `ignore`, `cancel`, `pending`, `skip`, `intercept[E]`, etc.
See also [Asserts.scala](https://github.com/wvlet/airframe/blob/master/airspec/src/main/scala/wvlet/airframe/spec/spi/Asserts.scala). 


## Running Tests in sbt

AirSpec supports pattern matching for running specific tests:
```
$ sbt

> test                                  # Run all tests
> testOnly -- (pattern)                 # Run all test matching the pattern (spec name or test name)
> testOnly -- (class pattern):(pattern)  # Search both class and test names 

> testOnly (class name pattern)         # Run all test classe matching the pattern
> testOnly *TestClassName -- (pattern)  # Run all matching tests in a specific class

```

`pattern` supports wildcard (`*`) and regular expressions. Cases of test classes and names will be ignored.
Basically this command finds matches from the list of all `(test class full name):(test function name)` strings.


## Writing Specs In Natural Languages

If you prefer natural language descriptions for your test cases, use symbols for function names:

```scala
import wvlet.airframe.spec._

class SeqSpec extends AirSpec {
  def `the size of empty Seq should be 0`: Unit = {
    assert(Seq.empty.size == 0)
  }

  // Catch an exception
  def `throw NoSuchElementException when taking the head of an empty Set`(): Unit = {
    intercept[NoSuchElementException] {
      Seq.empty.head
    }
  }
}
```

It is also possible to use Symbol for test class names:

```scala
import wvlet.airframe.spec._

class `Seq[X] test spec` extends AirSpec {
  def `the size of empty Seq should be 0`: Unit = {
    assert(Seq.empty.size == 0)
  }
}
```

## shouldBe matchers

AirSpec supports handy assertions with `shouldBe` or `shouldNotBe`:
```scala
import wvlet.airframe.spec._

class MyTest extends AirSpec {
  def test: Unit = {
    // checking the value equality with shouldBe, shouldNotBe:
    1 shouldBe 1
    1 shouldNotBe 2
    List().isEmpty shouldBe true

    // For optional values, shouldBe defined (or empty) can be used:
    Option("hello") shouldBe defined
    Option(null) shouldBe empty
    None shouldNotBe defined

    // For Arrays, shouldBe checks the equality with deep equals
    Array(1, 2) shouldBe Array(1, 2)

    // Collection checker 
    Seq(1) shouldBe defined
    Seq(1) shouldNotBe empty
    Seq(1, 2) shouldBe Seq(1, 2)
    (1, 'a') shouldBe (1, 'a')

    // Object equality checker
    val a = List(1, 2)
    val a1 = a
    val b = List(1, 2)
    a shouldBe a1
    a shouldBeTheSameInstanceAs a1
    a shouldBe b
    a shouldNotBeTheSameInstanceAs b
  }
}
```

## Scala.js

To use AirSpec in Scala.js, `scalaJsSupport` must be called inside your spec classes:
 
```scala
import wvlet.airframe.spec._

class ScalaJSSpec extends AirSpec {
  // This is necessary to find test methods in Scala.js
  scalaJsSupport  

  def myTest: Unit = assert(1 == 1)
}
```

Scala.js has no runtime reflection to find methods in AirSpec classes.
So calling `scalaJsSupport` will generate `MethodSurface`s (airframe-surface), so that 
AirSpec can find test methods at runtime. 

Calling `scalaJsSupport` has no effect in Scala JVM platform, so you can use the
same test spec both for Scala and Scala.js.


## Dependency Injection with Airframe DI

AirSpec can pass shared objects to your test cases by using function arguments.
This enables sharing objects initialized at the global session between the test cases in a test instance. 

## Global and Local Sessions

AirSpec manages two types of sessions: _global_ and _local_:
- For each AirSpec instance, a single global session will be created.
- For each test method in the AirSpec instance, a local (child) session that inherits the global session will be created.

To configure the design of objects that will be created in each session,
override `configure(Design)` and `configureLocal(Design)` methods in AirSpec.

### Session LifeCycle

AirSpec manages global/local sessions in this order:

- Create a new instance of AirSpec
  - Run `beforeAll`
  - Call configure(design) to prepare a new global design
  - Start a new global session
     - for each test method _m_:
       - Call `before`
       - Call configureLocal(design) to prepare a new local design
       - Start a new local session
          - Call the test method _m_ by building method arguments using the local session (and the global session). See also [Airframe DI: Child Sessions](https://wvlet.org/airframe/docs/airframe.html#child-sessions) to learn more about the dependency resolution order.
       - Shutdown the local session. All data in the local session will be discarded
       - Call `after`
     - Repeat the loop
  - Shutdown the global session. All data in the global session will be discarded.
  - Call `afterAll`

## DI Example

This is an example to utilize a global session to share the same service instance between test methods:
```scala
import wvlet.airframe.spec._
import wvlet.airframe.Design
import wvlet.log.LogSupport
import javax.annotation._

case class ServiceConfig(port:Int)

class Service(config:ServiceConfig) extends LogSupport {
  @PostConstruct
  def start {
    info(s"Starting a server at ${config.port}")
  }

  @PreDestroy
  def end {
    info(s"Stopping the server at ${config.port}")
  }
}

class ServiceSpec extends AirSpec with LogSupport {
  override protected def configure(design:Design): Design = {
    design
      .bind[Service].toSingleton
      .bind[ServiceConfig].toInstance(ServiceConfig(port=8080))
  }

  def test1(service:Service): Unit = {
     info(s"server id: ${service.hashCode}")
  }

  def test2(service:Service): Unit = {
     info(s"server id: ${service.hashCode}")
  }
}
```

This test shares the same Service instance between two test methods `test1` and `test2`:
```scala
> testOnly -- ServiceSpec
2019-08-09 17:24:37.184-0700  info [Service] Starting a server at 8080  - (ServiceSpec.scala:25)
2019-08-09 17:24:37.188-0700  info [ServiceSpec] test1: server id: 588474577  - (ServiceSpec.scala:42)
2019-08-09 17:24:37.193-0700  info [ServiceSpec] test2: server id: 588474577  - (ServiceSpec.scala:46)
2019-08-09 17:24:37.194-0700  info [Service] Stopping the server at 8080  - (ServiceSpec.scala:30)
[info] ServiceSpec:
[info]  - test1 13.94ms
[info]  - test2 403.41us
[info] Passed: Total 2, Failed 0, Errors 0, Passed 2
```

## Reusing Test Classes

To reuse test cases, create a fixture, which is a class, object, or trait extending
AirSpec. Then call AirSpecContext.run(AirSpec instance), which can be injected to test method arguments:

```scala
import wvlet.airframe.spec._
import wvlet.airframe.spec.spi.AirSpecContext

class MySpec extends AirSpec {
  // A template for reusable test cases
  class Fixture[A](data: Seq[A]) extends AirSpec {
    override protected def beforeAll: Unit = {
      info(s"Run tests for ${data}")
    }
    def emptyTest: Unit = {
      data shouldNotBe empty
    }
    def sizeTest: Unit = {
      data.length shouldBe data.size
    }
  }

  // Inject AirSpecContext using DI
  def test(context: AirSpecContext): Unit = {
    context.run(new Fixture(Seq(1, 2)))
    context.run(new Fixture(Seq("A", "B", "C")))
  }
}
```

This code will run the same Fixture two times using different data sets:
```
MySpec:
    MySpec.Fixture:
2019-08-15 15:11:33.458-0700  info [MySpec$Fixture] Run tests for List(1, 2)
     - emptyTest 471.97us
     - sizeTest 156.25us
    MySpec.Fixture:est / testOnly 0s
2019-08-15 15:11:33.475-0700  info [MySpec$Fixture] Run tests for List(A, B, C)
     - emptyTest 105.52us
     - sizeTest 80.27us
 - test 38.52ms
```

## Property Based Testing with ScalaCheck

Optionally AirSpec can integrate with [ScalaCheck](https://github.com/typelevel/scalacheck/blob/master/doc/UserGuide.md).
Add `wvlet.airframe.spec.spi.PropertyCheck` trait to your spec, and use `forAll` methods.

__build.sbt__
```scala
libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.14.0" % "test"
```

```scala
import wvlet.airframe.spec._
import wvlet.airframe.spec.spi.PropertyCheck

class PropertyBasedTest extends AirSpec with PropertyCheck {
  def testAllInt: Unit = {
    forAll{ i:Int => i.isValidInt shouldBe true }
  }

  def testCommutativity: Unit = {
    forAll{ (x:Int, y:Int) => x+y == y+x }
  }

  def useGenerator: Unit = {
    import org.scalacheck.Gen
    forAll(Gen.posNum[Long]){ x: Long => x > 0 shouldBe true }
  }
}
```
