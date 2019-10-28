AirSpec
======

[AirSpec](https://github.com/wvlet/airframe/tree/master/airspec) is a new functional testing framework for Scala and Scala.js. 

- [GitHub: AirSpec](https://github.com/wvlet/airframe/tree/master/airspec)

AirSpec uses pure Scala functions for writing test cases. This style requires no extra learning cost if you already know Scala. For advanced users, dependency injection to test cases and property-based testing are supported optionally.

AirSpec has nice properties for writing tests in Scala:

- Simple usage: `import wvlet.airspec._` and extend `AirSpec` trait.
- Tests can be defined using plain Scala classes and methods.
  - Public methods in a class extending `AirSpec` trait will be your test cases.
  - No annotation (like ones in [JUnit5](https://junit.org/junit5/docs/current/user-guide/)) is necessary.
- Testing with simple assertions: `assert(cond)`, `x shouldBe y`, etc.
  - No need to learn other complex DSLs.
- Lifecycle management with [Airframe DI](https://wvlet.org/airframe/docs/airframe.html):
  - The arguments of test methods can be used to inject necessary services for running your tests. 
  - The lifecycle (e.g., start and shutdown) of the injected services can be managed by Airframe DI.
- Nesting and reusing test cases with `context.run(spec)`
- Handy keyword search for _sbt_: `> testOnly -- (a pattern for class or method names)`
- Property-based testing integrated with [ScalaCheck](https://www.scalacheck.org/)
- Scala 2.11, 2.12, 2.13, and Scala.js support

AirSpec is already feature complete and ready to use in production. Actually, all modules of Airframe, including AirSpec, are tested by using AirSpec.
For providing better testing experience, we are now planning to add more features (e.g., better reporting, power assertions):
- [Milestone: AirSpec 19](https://github.com/wvlet/airframe/issues/606)


To start using AirSpec, read [Quick Start](#quick-start).

## Background & Motivation

In Scala there are several rich testing frameworks like [ScalaTests](http://www.scalatest.org/), [Specs2](https://etorreborre.github.io/specs2/), [uTest](https://github.com/lihaoyi/utest), etc. We also have a simple testing framework like [minitest](https://github.com/monix/minitest). In 2019, Scala community has started an experiment to creating a nano-testing framework [nanotest-strawman](https://github.com/scala/nanotest-strawman) based on minitest so that Scala users can have [some standards for writing tests in Scala](https://github.com/scala/scala-dev/issues/641) without introducing third-party dependencies.

A problem here is, in order to write tests in Scala, we usually have only two choices: learning DSLs or being a minimalist:

- __Complex DSLs__:
  - ScalaTests supports [various writing styles of tests](http://www.scalatest.org/user_guide/selecting_a_style), and [assersions](http://www.scalatest.org/user_guide/using_assertions). We had no idea how to choose the best style for our team. To simplify this, uTest has picked only one of the styles from ScalaTests, but it's still a new domain-specific language on top of Scala. Specs2 introduces its own testing syntax, and even [the very first example](https://etorreborre.github.io/specs2/) can be cryptic for new people, resulting in high learning cost.
  - With these rich testing frameworks, using a consistent writing style is challenging as they have too much flexibility in writing test; Using rich assertion syntax like `x should be (>= 0)`, `x.name must_== "me"`, `Array(1, 2, 3) ==> Array(1, 2, 3)`  needs practices and education within team members. We can force team members so as not to use them, but having a consensus on what can be used or not usually takes time.

- __Too minimalistic framework:__
  - On the other hand, a minimalist approach like minitest, which uses a limited set of syntax like `asserts` and `test("....")`, is too restricted. For example, I believe assertion syntax like `x shouldBe y` is a good invention in ScalaTest to make clear the meaning of assertions to represent `(value) shoudlBe (expected value)`. In minitest `assert(x == y)` has the same meaning, but the intention of test writers is not clear because we can write it in two ways: `assert(value == expected)` or `assert(expected == value)`. Minitest also has no feature for selecting test cases to run; It only supports specifying class names to run, which is just a basic functionality of __sbt__.
  - A minimalist approach forces us to be like [Zen](https://en.wikipedia.org/wiki/Zen) mode. We can extend minitest with rich assertions, but we still need to figure out the right balance between a minimalist and developing a DSL for our own teams.

## AirSpec: Writing Tests As Plain Functions In Scala

So where is a middle ground in-between these two extremes? We usually don't want to learn too complex DSLs, and also we don't want to be a minimalist, either.

Why can't we __use plain Scala functions to define tests__? ScalaTest already has [RefSpec](http://www.scalatest.org/user_guide/selecting_a_style) to write tests in Scala functions. Unfortunately, however, its support is limited only to Scala JVM as Scala.js does not support runtime reflections to list function names. Scala.js is powerful for developing web applications in Scala, so we don't want to drop it, and the lack of runtime reflection in Scala.js is probably a reason why existing testing frameworks needed to develop their own DSLs like `test(....) {  test body } `.

Now listing functions in Scala.js is totally possible by using [airframe-surface](https://wvlet.org/airframe/docs/airframe-surface.html), which is a library to inspect parameters and methods in a class by using reflection (in Scala JVM) or Scala macros (in Scala.js). So it was a good timing for us to develop a new testing framework, which has more Scala-friendly syntax. 

And also, if we define tests by using functions, it becomes possible to __pass test dependencies through function arguments__. Using local variables in a test class has been the best practice of setting up testing environments (e.g., database, servers, etc.), but it is not always ideal as we need to properly initialize and clean-up these variables for each test method by using setUp/tearDown (or before/after) methods. If we can simply pass these service instances to function arguments using [Airframe DI](https://wvlet.org/airframe/docs/airframe.html), which has a strong support of life-cycle management, we no longer need to write such setUp/tearDown steps for configuring testing environments. Once we define a production-quality service with proper lifecycle management hooks (using Airframe design and onStart/onShutdown hooks), we should be able to reuse these lifecycle management code even in test cases.

AirSpec was born with these ideas in mind by leveraging Airframe modules like Airframe Surface and DI. After implementing basic features of AirSpec, we've successfully __migrated all of the test cases in 20+ Airframe modules into AirSpec__, which were originally written in ScalaTest. Rewriting test cases was almost straightforward as AirSpec has handy `shouldBe` syntax and property testing support with ScalaCheck.

In the following sections, we will see how to write tests in a Scala-friendly style with AirSpec.

# Quick Start

To use AirSpec, add `"org.wvlet.airframe" %% "airspec"` to your test dependency and add `wvlet.airspec.Framework` as a TestFramework.

AirSpec uses `(year).(month).(patch)` versioning scheme. For example, version 19.8.x means a version released on August, 2019:

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airspec_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airspec_2.12/)

**build.sbt**
```scala
libraryDependencies += "org.wvlet.airframe" %% "airspec" % "(version)" % "test"
testFrameworks += new TestFramework("wvlet.airspec.Framework")
```

If you have multiple sub projects, add the above `testFramework` setting to each sub project.

For Scala.js, use `%%%`:
```scala
libraryDependencies += "org.wvlet.airframe" %%% "airspec" % "(version)" % "test"
testFrameworks += new TestFramework("wvlet.airspec.Framework")
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

### Scala.js

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


## Writing Specs In Natural Languages

If you prefer natural language descriptions for your test cases, use symbols (back-quoted strings) for function names:

```scala
import wvlet.airspec._

class SeqSpec extends AirSpec {
  def `the size of empty Seq should be 0`: Unit = {
    assert(Seq.empty.size == 0)
  }

  // Catch an exception
  def `throw NoSuchElementException when taking the head of an empty Set`: Unit = {
    intercept[NoSuchElementException] {
      Seq.empty.head
    }
  }
}
```

It is also possible to use symbols for test class names:

```scala
import wvlet.airspec._

class `Test properties of Seq[X]` extends AirSpec {
  def `the size of empty Seq should be 0`: Unit = {
    assert(Seq.empty.size == 0)
  }
}
```

## Assertions in AirSpec

AirSpec supports basic assertions listed below:

|__syntax__               | __meaning__ |
|-------------------------|----------|
|__assert(x == y)__         | check x equals to y |
|__assertEquals(a, b, delta)__ | check the equality of Float (or Double) values by allowing some delta difference |
|__intercept[E] { ... }__   | Catch an exception of type `E` to check an expected exception is thrown |
|__x shouldBe y__           | check x == y. This supports matching collections like Seq, Array (with deepEqual) |
|__x shouldNotBe y__        | check x != y |
|__x shouldNotBe null__     | shouldBe, shouldNotBe supports null check|
|__x shouldBe defined__     | check x.isDefined == true, when x is Option or Seq |
|__x shouldBe empty__       | check x.isEmpty == true, when x is Option or Seq |
|__x shouldBeTheSameInstanceAs y__ | check x eq y; x and y are the same object instance |
|__x shouldNotBeTheSameInstanceAs y__ | check x ne y; x and y should not be the same instance |
|__fail("reason")__         | fail the test if this code path should not be reached  |
|__ignore("reason")__       | ignore this test execution.  |
|__cancel("reason")__       | cancel the test (e.g., due to set up failure) |
|__pending__                | pending the test execution (e.g., when hitting an unknown issue) |
|__pendingUntil("reason")__ | pending until fixing some blocking issues|
|__skip("reason")__         | Skipping unnecessary tests (e.g., tests that cannot be supported in Scala.js) |

Tests in AirSpec are just regular functions in Scala. AirSpec is designed to use pure Scala syntax as much as possible so as not to introduce any complex DSLs, which are usually hard to remember.

### Examples

Here are examples of using `shouldBe` matchers:

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

## Running Tests in sbt

AirSpec supports pattern matching for running only specific tests:
```scala
$ sbt

> test                                   # Run all tests
> testOnly -- (pattern)                  # Run all test matching the pattern (class name or test name)
> testOnly -- (class pattern)*(pattern)  # Search both class and test names

# sbt's default test functionalities:
> testQuick                              # Run only previously failed test specs
> testOnly (class name)                  # Run tests only in specific classes matching a pattern (wildcard is supported) 
```

`pattern` is used for partial matching with test names. It also supports wildcard (`*`) and regular expressions (experimental).
Basically AirSpec will find matches from the list of all `(test class full name):(test function name)` strings.
Cases of test names will be ignored in the search.

![image](https://wvlet.org/airframe/img/airspec/airspec.png)

### Disable Parallel Test Execution

sbt 1.x or higher runs tests in parallel. This is fast, but it messes up console log messages.
If you prefer sequential test execution, set `parallelExecution in Test` to false:

```scala
parallelExecution in Test := false
```

## Logging Your Tests

To debug your tests, showing console logs with `info`, `debug`, `trace`, `warn`, `error` functions will be useful. AirSpec is integrated with [airframe-log](https://wvlet.org/airframe/docs/airframe-log.html) to support handly logging messages with these functions.

```scala
import wvlet.airspec._

package org.mydomain.myapp

class MyTest extends AirSpec {
  info(s"info log")
  debug(s"debug log") // Will not be shown by default 
  trace(s"trace log") // To show this level of log, trace log level must be set
}
```

Similarly, if you include `wvlet.log.LogSupport` to your application classes, you can add log messages to these classes.

### Changing Log Levels

To change the log level for your packages and classes, add _log-test.properties_ file to your test resource folder `src/test/resources`. For multi-module projects, put this file under `(project folder)/src/test/resources` folder.

This is an example of changing log levels of your packages and classes:

__src/test/resources/log-test.properties__
```
# Log level configuration examples
# Show debug logs of all classes in org.mydomain.myapp package 
org.mydomain.myapp=debug
# Show  
org.mydomain.myapp.MyTest=trace
```

As you modify this property file, a background thread automatically reads this log file and refreshes the log level accordingly.

For more details, see the [documentation](https://wvlet.org/airframe/docs/airframe-log.html) of airframe-log.

## Dependency Injection with Airframe DI

AirSpec can pass shared objects to your test cases by using function arguments.
This is useful for sharing objects initialized only once at the beginning with test cases.

### Global and Local Sessions

AirSpec manages two types of sessions: _global_ and _local_:
- For each AirSpec instance, a single global session will be created.
- For each test method in the AirSpec instance, a local (child) session that inherits the global session will be created.

To configure the design of objects that will be created in each session,
override `protected def design:Design` or `protected def localDesign: Design` methods in AirSpec.

### Session LifeCycle

AirSpec manages global/local sessions in this order:

- Create a new instance of AirSpec
  - Run `beforeAll`
  - Call `design` to prepare a new global design
  - Start a new global session
     - for each test method _m_:
       - Call `before`
       - Call `localDesign` to prepare a new local design
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
class Service(val config:ServiceConfig)

class ServiceSpec extends AirSpec {
  override protected def design: Design = {
    newDesign
      .bind[ServiceConfig].toInstance(ServiceConfig(port=8080))
      .bind[Service].toSingleton
      .onStart{x => info(s"Starting a server at ${x.config.port}")}
      .onShutdown{x => info(s"Stopping the server at ${x.config.port}")}
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
2019-08-09 17:24:37.184-0700  info [ServiceSpec] Starting a server at 8080  - (ServiceSpec.scala:33)
2019-08-09 17:24:37.188-0700  info [ServiceSpec] test1: server id: 588474577  - (ServiceSpec.scala:42)
2019-08-09 17:24:37.193-0700  info [ServiceSpec] test2: server id: 588474577  - (ServiceSpec.scala:46)
2019-08-09 17:24:37.194-0700  info [ServiceSpec] Stopping the server at 8080  - (ServiceSpec.scala:34)
[info] ServiceSpec:
[info]  - test1 13.94ms
[info]  - test2 403.41us
[info] Passed: Total 2, Failed 0, Errors 0, Passed 2
```

### Overriding Design At Test Methods

If you need a partially different design in a test method, pass Airframe `Session` to the test method arguments, and call `session.withChildSession(additional_design)`:

```scala
import wvlet.airspec._
import wvlet.airframe._

class OverrideTest extends AirSpec {

  override protected def design: Design = {
    newDesign
      .bind[String].toInstance("hello")
  }
 
  // Pass Session to override the design
  def overrideDesign(session: Session, s: String): Unit = {
    s shouldBe "hello"
  
    // Override a design
    val d = newDesign
      .bind[String].toInstance("hello child")
   
    // Create a new child session
    session.withChildSession(d) { childSession =>
      val cs = childSession.build[String]
      cs shouldBe "hello child"
    }
  }
}

```

### Pro Tips

Designs of Airframe DI are useful for defining modules of your application.
If you need to switch some implementations or configurations for your tests, override your design as shown below:

```scala
import wvlet.airframe._

object AppModule {
  // Define your application design
  def serviceDesign: Design = {
    newDesign
        .bind[Service].to[ServiceImpl]
        .bind[ServiceConfig].toInstance(new ServiceConfig(...))
  }

  // Define designs of other compoments as you need
  def componentDesign: Design = ...
}

// Design for testing
object AppTestModule {
  def serviceDesignForTests: Design = {
    AppModule.serviceDesign  // Reuse your application design for tests
     .bind[ServiceConfig].toInstnce(new ServiceConfig(...)) // Override the config for tests
  }
}

import wvlet.airspec._
class AppTest extends AirSpec {
  // Use the testing design
  protected override def design = AppTestModule.serviceDesignForTests

  // Inject a Service object initialized with a test configuration
  def `start up test`(service:Service): Unit = {
     // test your service here
  }
}
```

If you are already familiar to dependency injection using [Airframe DI](https://wvlet.org/airframe/docs/airframe.html) or [Google Guice](https://github.com/google/guice), it would not be so difficult to split your application into some units of testable modules.
 This is generally a good practice to minimize the scope of tests only for specific components.

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
// Use %%% for Scala.js
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.1" % "test"
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

