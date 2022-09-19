---
id: airspec
layout: docs
title: AirSpec: Testing Framework
---

[AirSpec](https://github.com/wvlet/airframe/tree/master/airspec) is a new functional testing framework for Scala and Scala.js.

- [GitHub: AirSpec](https://github.com/wvlet/airframe/tree/master/airspec)
- [Background and Motivation](#background--motivation)

AirSpec uses just `test("...") { ... }` syntax for writing test cases. This style requires no extra learning cost if you already know Scala. For advanced users, dependency injection to test cases and property-based testing are supported optionally.

## Features

- Simple to use: Just `import wvlet.airspec._` and extend `AirSpec` trait.
- Tests can be defined with `test(...)` functions.
  - No annotation is required like ones in [JUnit5](https://junit.org/junit5/docs/current/user-guide/)) is necessary.
- Support basic assertion syntaxes: `assert(cond)`, `x shouldBe y`, etc.
  - No need to learn other complex DSLs.
- Nesting and reusing test cases with `test(...)`
- Async testing support for `scala.concurrent.Future[ ]`
- Lifecycle management with [Airframe DI](airframe-di.md):
  - DI will inject the arguments of test methods based on your custom Design.
  - The lifecycle (e.g., start and shutdown) of the injected services will be properly managed.
- Handy keyword search for _sbt_: `> testOnly -- (a pattern for class or method names)`
- Property-based testing integrated with [ScalaCheck](https://www.scalacheck.org/)
- Scala 2.12, 2.13, 3.0, and Scala.js support

To start using AirSpec, read [Quick Start](#quick-start).

## Quick Start

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

In AirSpec, you can describe test cases with `test(...) { body }` in a class (or an object) extending `AirSpec`.

```scala
import wvlet.airspec._

class MyTest extends AirSpec {
  test("empty Seq size should be 0") {
    assert(Seq.empty.size == 0)
  }

  test("Seq.empty.head should fail") {
    intercept[NoSuchElementException] {
      Seq.empty.head
    }
  }
}
```

This `test` syntax is useful for writing nested tests or customizing the design of DI for each test.


## Assertions in AirSpec

AirSpec supports basic assertions listed below:

|syntax               | meaning |
|-------------------------|----------|
|`assert(x == y)`         | check x equals to y |
|`assertEquals(a, b, delta)` | check the equality of Float (or Double) values by allowing some delta difference |
|`intercept[E] { ... }`   | Catch an exception of type `E` to check an expected exception is thrown |
|`x shouldBe y`           | check x == y. This supports matching collections like Seq, Array (with deepEqual) |
|`x shouldNotBe y`        | check x != y |
|`x shouldNotBe null`     | shouldBe, shouldNotBe supports null check|
|`x shouldBe defined`     | check x.isDefined == true, when x is Option or Seq |
|`x shouldBe empty`       | check x.isEmpty == true, when x is Option or Seq |
|`x shouldBeTheSameInstanceAs y` | check x eq y; x and y are the same object instance |
|`x shouldNotBeTheSameInstanceAs y` | check x ne y; x and y should not be the same instance |
|`fail("reason")`         | fail the test if this code path should not be reached  |
|`ignore("reason")`       | ignore this test execution.  |
|`cancel("reason")`       | cancel the test (e.g., due to set up failure) |
|`pending`                | pending the test execution (e.g., when hitting an unknown issue) |
|`pendingUntil("reason")` | pending until fixing some blocking issues|
|`skip("reason")`         | Skipping unnecessary tests (e.g., tests that cannot be supported in Scala.js) |

AirSpec is designed to use pure Scala syntax as much as possible so as not to introduce any complex DSLs, which are usually hard to remember.

### Examples

Here are examples of using `shouldBe` matchers:

```scala
import wvlet.airspec._

class MyTest extends AirSpec {
  test("assertion examples") {
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

```ruby
# Show debug logs of all classes under org.mydomain.myapp package
org.mydomain.myapp=debug
# Show all logs including trace-level logs
org.mydomain.myapp.MyTest=trace
```

As you modify this property file, a background thread automatically reads this log file and refreshes the log level accordingly.

For more details, see the [documentation](https://wvlet.org/airframe/docs/airframe-log.html) of airframe-log.

## Detecting Runtime Environment

For detecting the running environment of tests, the following methods are available: 

- inCI: Boolean
- inTravisCI: Boolean
- inCircleCI: Boolean
- inGitHubAction: Boolean
- isScalaJS: Boolean
- isScala2: Boolean
- isScala3: Boolean
- scalaMajorVersion: Int

For example:
```scala
test("scala-3 specific tests") {
  if(isScala3) {
    scalaMajorVersion shoudlBe 3 
  }  
}
```

## Async Testing

Since the version 22.5.0, AirSpec supports tests returning `Future[_]` values. Such async tests are useful, especially if your application needs to wait the completion of network requests, such as Ajax responses in Scala.js, RPC responses from a server, etc. If test specs returns `Future` values, AirSpec awaits the completion of async tests, so you don't need to write synchronization steps in your test code.


```scala
import wvlet.airspec._
import scala.concurrent.{Future,ExecutionContext}

class AsyncTest extends AirSpec {
  // Use the default ExecutionContext for running Future tasks.
  private implicit val ec: ExecutionContext = defaultExecutionContext

  // AirSpec awaits the completion of the returned Future value
  test("async test") {
    Future.apply {
       println("hello async test")
    }
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
  - It is possible to override the local design by using `test(..., design = ...)` function.

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

  test("test1") { (service:Service) =>
     info(s"server id: ${service.hashCode}")
  }

  test("test2") { (service:Service) =>
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

It is also possible to reuse the same injected instance by nesting `test` methods:

```scala
class ServiceSpec extends AirSpec {
  override protected def design: Design = ...
  test("server test") { service:Service =>
    test("test 1") {
      info(s"server id: ${service.hashCode}")
    }

    test("test 2") {
     info(s"server id: ${service.hashCode}")
    }
  }
}

```

### Overriding Design At Test Methods

If you need to partially override a design in a test method, use `test(..., design = ...)` to provide a custom child design:

```scala
import wvlet.airspec._
import wvlet.airframe._

class OverrideTest extends AirSpec {

  override protected def design: Design = {
    newDesign
      .bind[String].toInstance("hello")
  }

  // Pass Session to override the design
  test("before overriding the design") { (s:String) =>
    s shouldBe "hello"

    // Override a design
    val childDesign = newDesign
      .bind[String].toInstance("hello child")

    test("override the design", design = childDesign) { cs: String =>
      cs shouldBe "hello child"
    }
  }
}
```

### FAQs

Q: MissingTestDependency error is shown for a test without any argument block

For example, this test throws an error:
```scala
// wvlet.airspec.spi.MissingTestDependency: Failed to call `return a function`. Missing dependency for Int:
test("return a function") {
  val f = { (i: Int) => s"count ${i}" }
  f
}
```

This is because the above code is equivalent to having one dependency argument like this:
```scala
test("return a function") { (i: Int) =>
  // ...
}
```

Workaround: Specify [Unit] type:
```scala
test("return a function")[Unit] {
  val f = { (i: Int) => s"count ${i}" }
  f
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
  test("start up test") { (service:Service) =>
     // test your service here
  }
}
```

If you are already familiar to dependency injection using [Airframe DI](https://wvlet.org/airframe/docs/airframe.html) or [Google Guice](https://github.com/google/guice), it would not be so difficult to split your application into some units of testable modules.
 This is generally a good practice to minimize the scope of tests only for specific components.


This code will run the same Fixture two times using different data sets:
![image](https://wvlet.org/airframe/img/airspec/nesting.png)

AirSpecContext also contains the name of test classes and method names, which would be useful to know which tests are currently running.

## Property Based Testing with ScalaCheck

Optionally AirSpec can integrate with [ScalaCheck](https://github.com/typelevel/scalacheck/blob/master/doc/UserGuide.md).
Add `wvlet.airspec.spi.PropertyCheck` trait to your spec, and use `forAll` methods.

__build.sbt__
```scala
// Use %%% for Scala.js
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.4" % "test"
```

```scala
import wvlet.airspec._

class PropertyBasedTest extends AirSpec with PropertyCheck {
  test("testAllInt") {
    forAll{ (i:Int) => i.isValidInt shouldBe true }
  }

  test("testCommutativity") {
    forAll{ (x:Int, y:Int) => x+y == y+x }
  }

  // Using custom genrators of ScalaCheck
  test("useGenerator") {
    import org.scalacheck.Gen
    forAll(Gen.posNum[Long]){ x: Long => x > 0 shouldBe true }
  }
}
```

## Background & Motivation

In Scala there are several rich testing frameworks like [ScalaTests](http://www.scalatest.org/), [Specs2](https://etorreborre.github.io/specs2/), [uTest](https://github.com/lihaoyi/utest), etc. We also have a simple testing framework like [minitest](https://github.com/monix/minitest). In 2019, Scala community has started an experiment to creating a nano-testing framework [nanotest-strawman](https://github.com/scala/nanotest-strawman) based on minitest so that Scala users can have [some standards for writing tests in Scala](https://github.com/scala/scala-dev/issues/641) without introducing third-party dependencies.

A problem here is, in order to write tests in Scala, we usually have only two choices: learning DSLs or being a minimalist:

- __Complex DSLs__:
  - ScalaTests supports [various writing styles of tests](http://www.scalatest.org/user_guide/selecting_a_style), and [assersions](http://www.scalatest.org/user_guide/using_assertions). We had no idea how to choose the best style for our team. To simplify this, uTest has picked only one of the styles from ScalaTests, but it's still a new domain-specific language on top of Scala. Specs2 introduces its own testing syntax, and even [the very first example](https://etorreborre.github.io/specs2/) can be cryptic for new people, resulting in high learning cost.
  - With these rich testing frameworks, using a consistent writing style is challenging as they have too much flexibility in writing test; Using rich assertion syntax like `x should be (>= 0)`, `x.name must_== "me"`, `Array(1, 2, 3) ==> Array(1, 2, 3)`  needs practices and education within team members. We can force team members so as not to use them, but having a consensus on what can be used or not usually takes time.

- __Too minimalistic framework:__
  - On the other hand, a minimalist approach like minitest, which uses a limited set of syntax like `asserts` and `test("....")`, is too restricted. For example, I believe assertion syntax like `x shouldBe y` is a good invention in ScalaTest to make clear the meaning of assertions to represent `(value) shoudlBe (expected value)`. In minitest `assert(x == y)` has the same meaning, but the intention of test writers is not clear because we can write it in two ways: `assert(value == expected)` or `assert(expected == value)`. Minitest also has no feature for selecting test cases to run; It only supports specifying class names to run, which is just a basic functionality of __sbt__.
  - A minimalist approach forces us to be like [Zen](https://en.wikipedia.org/wiki/Zen) mode. We can extend minitest with rich assertions, but we still need to figure out the right balance between a minimalist and developing a DSL for our own teams.

### AirSpec: Writing Tests As Plain Functions In Scala

So where is a middle ground in-between these two extremes? We usually don't want to learn too complex DSLs, and also we don't want to be a minimalist, either.

Why can't we __use plain Scala functions to define tests__? ScalaTest already has [RefSpec](http://www.scalatest.org/user_guide/selecting_a_style) to write tests in Scala functions. Unfortunately, however, its support is limited only to Scala JVM as Scala.js does not support runtime reflections to list function names. Scala.js is powerful for developing web applications in Scala, so we don't want to drop it, and the lack of runtime reflection in Scala.js is probably a reason why existing testing frameworks needed to develop their own DSLs like `test(....) {  test body } `.

Now listing functions in Scala.js is totally possible by using [airframe-surface](https://wvlet.org/airframe/docs/airframe-surface.html), which is a library to inspect parameters and methods in a class by using reflection (in Scala JVM) or Scala macros (in Scala.js). So it was a good timing for us to develop a new testing framework, which has more Scala-friendly syntax.

And also, if we define tests by using functions, it becomes possible to __pass test dependencies through function arguments__. Using local variables in a test class has been the best practice of setting up testing environments (e.g., database, servers, etc.), but it is not always ideal as we need to properly initialize and clean-up these variables for each test method by using setUp/tearDown (or before/after) methods. If we can simply pass these service instances to function arguments using [Airframe DI](https://wvlet.org/airframe/docs/airframe.html), which has a strong support of life-cycle management, we no longer need to write such setUp/tearDown steps for configuring testing environments. Once we define a production-quality service with proper lifecycle management hooks (using Airframe design and onStart/onShutdown hooks), we should be able to reuse these lifecycle management code even in test cases.

AirSpec was born with these ideas in mind by leveraging Airframe modules like Airframe Surface and DI. After implementing basic features of AirSpec, we've successfully __migrated all of the test cases in 20+ Airframe modules into AirSpec__, which were originally written in ScalaTest. Rewriting test cases was almost straightforward as AirSpec has handy `shouldBe` syntax and property testing support with ScalaCheck.

In the following sections, we will see how to write tests in a Scala-friendly style with AirSpec.
