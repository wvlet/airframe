AirSpec
======

[AirSpec](https://wvlet.org/airframe/docs/airspec.html) is a new functional testing framework for Scala and Scala.js. 

AirSpec uses pure Scala functions for writing test cases. This style requires no extra learning costs if you alreday know Scala. For advanced users, dependency injection and property-based testing are also available. 

Here is a summary of AirSpec features;

- Simple entry point: `import wvlet.airspec._`
- You can use plain Scala classes and methods to define tests.
  - Public methods in a class extending `AirSpec` trait will be your test cases.
  - No annotation is necessary.
- Simple assertions: `assert(cond)` or `x shouldBe y`
  - No need to remember complex DSLs
- Lifecycle management with [Airframe DI](https://wvlet.org/airframe/docs/airframe.html)
  - The arguments of test methods can be used to inject necessary services for running your tests. 
  - The lifecycle (e.g., start and shutdown) of the injected services can be managed by Airframe DI.
- Nesting and reusing your test cases with `context.run(spec)`
- Handy keyword search for _sbt_: `> testOnly -- (a pattern for class or method names)`
- Property-based testing integrated with [ScalaCheck](https://www.scalacheck.org/)
- Scala 2.11, 2.12, 2.13, and Scala.js support

# Motivation

In Scala there are several rich testing frameworks like [ScalaTests](http://www.scalatest.org/), [Specs2](https://etorreborre.github.io/specs2/), [uTest](https://github.com/lihaoyi/utest), etc. We also have a simple testing framework like [minitest](https://github.com/monix/minitest). In 2019, Scala community has started an experiment to create a nano-testing framework [nanotest-strawman](https://github.com/scala/nanotest-strawman) based on minitest so that Scala users can have some standards of running tests without introducing third-party dependencies.

__Problem__: These testing frameworks are good enough for writing tests, but in order to uses one of them, I and my team needed to learn complex DSLs, or to be a minimalist for using minimum testing frameworks like minitest.

- __Complex DSLs__:
  - ScalaTests supports [variosu writing styles of tests](http://www.scalatest.org/user_guide/selecting_a_style), and [assersions](http://www.scalatest.org/user_guide/using_assertions). We had no idea how to choose the best style for our team.
  - Specs2 introduces its own testing syntaxes, and even [the very first example](https://etorreborre.github.io/specs2/) is cryptic for new people (i.e., high learning cost).
  - With these rich testing frameworks, using a consistent style is challenging as we have too much flexibility in writing tests. And also remembering rich assertion syntaxes like `x should be (>= 0)` or `x.name must_== "me"` needs a lot of practices and education within the team.

- __Too Minimalistic Framework__
  - On the other hand, minitest can do the job of writing tests with a small set of syntaxes like `asserts` and `test("....")`, but its functionality is quite limited; For example, we can't run tests by specifying their names (only class name based search is supported in sbt). We also don't have useful syntax sugars like `x shouldBe y`. minitest is simple to use, but at the same time it forces us to be like [Zen](https://en.wikipedia.org/wiki/Zen)-mode.

## AirSpec: Writing Tests As Functions In Scala

Where is a middle ground betweeen these two extremes? I don't want to learn too complex DSLs, and also don't want to be a minimalist, neither.

My question was: Can we __use plain Scala functions to define tests__? From the experiences of developing [airframe-surface](https://wvlet.org/airframe/docs/airframe-surface.html) I've learned how to list functions in a class using reflection or Scala macros (in Scala.js). With this approach, a class in Scala will be just a test suite that has a set of test methods, and no need exists to introduce various testing styles as in ScalaTests.

And also, if we define tests by usign functions, it makes possible to __pass test dependencies through function arguments__. Using local variables in a test class has been the best practice of setting up testing environments (e.g., database, servers, etc.), but it was not ideal as we need to properly initalize and shutdown these variables with setUp/tearDown (or before/after) methods. If we can simply pass these service instances to function arguments using [Airframe DI](https://wvlet.org/airframe/docs/airframe.html), which has a strong support of life-cycle management, I thought we no longer need to write such setUp/tearDown steps for configuring testing environments. Once we define a production-quality service with proper lifecycle management hooks (using Airframe design and onStart/onShutdown hooks), we should be able to reuse these lifecycle management code even in test cases.

With these ideas in my mind, I've developed AirSpec by leveraging existing Airframe modules like surface and airframe DI. After implementing basic features of AirSpec, I've successfully __replaced all of test cases in 20+ Airframe modules with AirSpec__, which were originally written in ScalaTest. Rewriting test cases using AirSpec was almost straightforward as AirSpec has handy `shouldBe` syntaxes (for limited use cases) and property testing support with ScalaCheck. 

Let's start using AirSpec in the following tutorial.

# Quick Start

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airspec_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airspec_2.12/)

**build.sbt**
```
libraryDependencies += "org.wvlet.airframe" %% "airspec" % "(version)"
testFrameworks += new TestFramework("wvlet.airspec.Framework")
```

For Scala.js, use `%%%`:
```
libraryDependencies += "org.wvlet.airframe" %%% "airspec" % "(version)"
```

## Writing Unit Tests 

In AirSpec test cases are defined as functions in a class (or an object) extending `AirSpec`.
All public functions (methods) in the class will be executed as test cases:

```scala
import wvlet.airspec._

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

AirSpec supports basic assertions like `assert`, `fail`, `ignore`, `cancel`, `pending`, `skip`, `intercept[E]`, and `shouldBe` matchers (explained later). 

Tests in AirSpec are just regular functions in Scala. AirSpec is designed to use pure Scala syntax as much as possible so as not to introduce any complex DSLs, which are usually hard to remember.

AirSpec also supports powerful object lifecycle management, integrated with [Airframe DI](https://wvlet.org/airframe/docs/airframe.html). The function arguments of test methods will be used for injecting objects that are necessary for running tests, and after finishing tests, these objects will be discarded properly.


## Running Tests in sbt

AirSpec supports pattern matching for running specific tests:
```
$ sbt

> test                                  # Run all tests
> testOnly -- (pattern)                 # Run all test matching the pattern (spec name or test name)
> testOnly -- (class pattern)*(pattern)  # Search both class and test names 

```

`pattern` is used for partial matching with test names. It also supports wildcard (`*`) and regular expressions. 
Basically AirSpec will find matches from the list of all `(test class full name):(test function name)` strings.
Cases of test names will be ignored in the serach.

![image](https://wvlet.org/airframe/img/airspec/airspec.png)

## Writing Specs In Natural Languages

If you prefer natural language descriptions for your test cases, use symbols for function names:

```scala
import wvlet.airspec._

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
import wvlet.airspec._

class `Seq[X] test spec` extends AirSpec {
  def `the size of empty Seq should be 0`: Unit = {
    assert(Seq.empty.size == 0)
  }
}
```

## shouldBe matchers

AirSpec supports handy assertions with `shouldBe` or `shouldNotBe`:
```scala
import wvlet.airspec._

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
import wvlet.airspec._

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
Add `wvlet.airspec.spi.PropertyCheck` trait to your spec, and use `forAll` methods.

__build.sbt__
```scala
# Use %%% for Scala.js
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
```

```scala
import wvlet.airspec._

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

## Scala.js

To use AirSpec in Scala.js, `scalaJsSupport` must be called inside your spec classes:
 
```scala
import wvlet.airspec._

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

