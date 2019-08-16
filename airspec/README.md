AirSpec
======

[AirSpec](https://github.com/wvlet/airframe/tree/master/airspec) is a new functional testing framework for Scala and Scala.js. 

AirSpec uses pure Scala functions for writing test cases. This style requires no extra learning cost if you already know Scala. For advanced users, dependency injection to test cases and property-based testing are optionally supported.

AirSpec has nice properties for writing tests in Scala:

- A short entry point: `import wvlet.airspec._`
- Using plain Scala classes and methods to define tests.
  - Public methods in a class extending `AirSpec` trait will be your test cases.
  - No annotations (like [JUnit5](https://junit.org/junit5/docs/current/user-guide/)) are necessary.
- Testing with simple assertions: `assert(cond)` and `x shouldBe y`.
  - No need to learn other complex DSLs.
- Lifecycle management with [Airframe DI](https://wvlet.org/airframe/docs/airframe.html):
  - The arguments of test methods can be used to inject necessary services for running your tests. 
  - The lifecycle (e.g., start and shutdown) of the injected services can be managed by Airframe DI.
- Nesting and reusing test cases with `context.run(spec)`
- Handy keyword search for _sbt_: `> testOnly -- (a pattern for class or method names)`
- Property-based testing integrated with [ScalaCheck](https://www.scalacheck.org/)
- Scala 2.11, 2.12, 2.13, and Scala.js support

We are now planning to add some missing features (e.g., better reporting, power assertions):
- [Milestone: AirSpec 19](https://github.com/wvlet/airframe/issues/606)

# Motivation

In Scala there are several rich testing frameworks like [ScalaTests](http://www.scalatest.org/), [Specs2](https://etorreborre.github.io/specs2/), [uTest](https://github.com/lihaoyi/utest), etc. We also have a simple testing framework like [minitest](https://github.com/monix/minitest). In 2019, Scala community has started an experiment to creating a nano-testing framework [nanotest-strawman](https://github.com/scala/nanotest-strawman) based on minitest so that Scala users can have [some standards for writing tests in Scala](https://github.com/scala/scala-dev/issues/641) without introducing third-party dependencies.

A problem here is, in order to write tests in Scala, we usually have only two choices: learning DSLs or being a minimalist.

- __Complex DSLs__:
  - ScalaTests supports [various writing styles of tests](http://www.scalatest.org/user_guide/selecting_a_style), and [assersions](http://www.scalatest.org/user_guide/using_assertions). We had no idea how to choose the best style for our team. To simplify this, uTest has picked only one of the styles from ScalaTests, but it's still a new domain-specific language on top of Scala. Specs2 introduces its own testing syntax, and even [the very first example](https://etorreborre.github.io/specs2/) can be cryptic for new people, resulting in high learning cost.
  - With these rich testing frameworks, using a consistent writing style is challenging as they have too much flexibility in writing test; Using rich assertion syntax like `x should be (>= 0)`, `x.name must_== "me"`, `Array(1, 2, 3) ==> Array(1, 2, 3)`  needs practices and education within team members. We can force team members so as not to use them, but having a consensus on what can be used or not usually takes time.

- __Too minimalistic framework:__
  - On the other hand, a minimalist approach like minitest, which uses a limited set of syntax like `asserts` and `test("....")`, is too restricted. For example, I believe assertion syntax like `x shouldBe y` is a good invention in ScalaTest to make clear the meaning of assertions to represent `(value) shoudlBe (expected value)`. In minitest `assert(x == y)` has the same meaning, but the intention of test writers is not clear because we can write it in two ways: `assert(value == expected)` or `assert(expected == value)`. Minitest also has no feature for selecting test cases to run; It only supports specifying class names to run, which is just a basic functionality of __sbt__.
  - A minimalist approach like this forces us to be in [Zen](https://en.wikipedia.org/wiki/Zen) mode. We can extend minitest with rich assertions, but we need to figure out the right balance between a minimalist and developing a DSL for our own teams.

## AirSpec: Writing Tests As Functions In Scala

So where is a middle ground in-between these two extremes? We usually don't want to learn too complex DSLs, and also we don't want to be a minimalist, either.

Why can't we __use plain Scala functions to define tests__? ScalaTest already has [RefSpec](http://www.scalatest.org/user_guide/selecting_a_style) to write tests in Scala functions. Unfortunately, however, its support is limited only to Scala JVM as Scala.js does not support runtime reflections to list function names. Scala.js is powerful for developing web applications in Scala, so we don't want to drop it, and the lack of runtime reflection in Scala.js is probably a reason why existing testing frameworks needed to develop their own DSLs like `test(....) {  test body } `.

Now listing functions in Scala.js is totally possible by using [airframe-surface](https://wvlet.org/airframe/docs/airframe-surface.html), which is a library to inspect parameters and methods in a class by using reflection (in Scala JVM) or Scala macros (in Scala.js). So it was a good timing for us to develop a new testing framework, which has more Scala-friendly syntax. 

And also, if we define tests by using functions, it becomes possible to __pass test dependencies through function arguments__. Using local variables in a test class has been the best practice of setting up testing environments (e.g., database, servers, etc.), but it is not always ideal as we need to properly initialize and clean-up these variables for each test method by using setUp/tearDown (or before/after) methods. If we can simply pass these service instances to function arguments using [Airframe DI](https://wvlet.org/airframe/docs/airframe.html), which has a strong support of life-cycle management, we no longer need to write such setUp/tearDown steps for configuring testing environments. Once we define a production-quality service with proper lifecycle management hooks (using Airframe design and onStart/onShutdown hooks), we should be able to reuse these lifecycle management code even in test cases.

AirSpec was born with these ideas in mind by leveraging Airframe modules like Airframe Surface and DI. After implementing basic features of AirSpec, we've successfully __migrated all of the test cases in 20+ Airframe modules into AirSpec__, which were originally written in ScalaTest. Rewriting test cases was almost straightforward as AirSpec has handy `shouldBe` syntax and property testing support with ScalaCheck.

In the following sections, we will see how to use AirSpec to write tests in a Scala-friendly style.

# Quick Start

To use AirSpec, add `airspec` to your test dependency and add `wvlet.airspec.Framework` as a TestFramework: 

**build.sbt**

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airspec_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airspec_2.12/)
```scala
libraryDependencies += "org.wvlet.airframe" %% "airspec" % "(version)" % "test"
testFrameworks += new TestFramework("wvlet.airspec.Framework")
```

For Scala.js, use `%%%`:
```scala
libraryDependencies += "org.wvlet.airframe" %%% "airspec" % "(version)" % "test"
```

## Writing Unit Tests 

In AirSpec test cases are defined as functions in a class (or an object) extending `AirSpec`.
All __public methods__ in the class will be executed as test cases:

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

If you need to define utility methods in a class, use private or protected scope.

AirSpec supports basic assertions like `assert`, `fail`, `ignore`, `cancel`, `pending`, `skip`, `intercept[E]`, and `shouldBe` matchers, which is will be explained later.

Tests in AirSpec are just regular functions in Scala. AirSpec is designed to use pure Scala syntax as much as possible so as not to introduce any complex DSLs, which are usually hard to remember.

AirSpec also supports powerful object lifecycle management, integrated with [Airframe DI](https://wvlet.org/airframe/docs/airframe.html). The function arguments of test methods will be used for injecting objects that are necessary for running tests, and after finishing tests, these objects will be discarded properly.


## Running Tests in sbt

AirSpec supports pattern matching for running only specific tests:
```
$ sbt

> test                                   # Run all tests
> testOnly -- (pattern)                  # Run all test matching the pattern (class name or test name)
> testOnly -- (class pattern)*(pattern)  # Search both class and test names 

```

`pattern` is used for partial matching with test names. It also supports wildcard (`*`) and regular expressions.
Basically AirSpec will find matches from the list of all `(test class full name):(test function name)` strings.
Cases of test names will be ignored in the search.

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

It is also possible to use symbols for test class names:

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

    // null check
    val s:String = null
    s shouldBe null
    "s" shouldNotBe null

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
This is useful for sharing objects initialized only once at the beginning with test cases.

### Global and Local Sessions

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
import wvlet.airspec._
import wvlet.airframe._

case class ServiceConfig(port:Int)

class Service(config:ServerConfig) extends LogSupport {
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

This test shares the same Service instance between two test methods `test1` and `test2`, and properly start and closes the service before and after running tests.
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
![image](https://wvlet.org/airframe/img/airspec/nesting.png)

AirSpecContext also contains the name of test classes and method names, which would be useful to know which tests are currently running.

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

  // Using custom genrators of ScalaCheck
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

This is because Scala.js has no runtime reflection to find methods in AirSpec classes, so we need to provide method data by calling `scalaJsSupport`.
Internally this will generate `MethodSurface`s (airframe-surface), so that AirSpec can find test methods at runtime. Calling `scalaJsSupport` has no effect in Scala JVM platform, so you can use the same test spec both for Scala and Scala.js.

