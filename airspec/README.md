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
- Create a class (or object) by extending `wvlet.airframe.spec.AirSpec`.
- Define your test cases as functions (public methods) in this class.
- Use sbt `> testOnly -- (pattern)` to run your tests!

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
`pattern` supports wildcard (`*`) and regular expressions. Cases will be ignored.  


## Writing Specs In Natural Languages

If you prefer natural language descriptions for your test cases, use symbols for function names:

```scala
import wvlet.airframe.spec._

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

In AirSpec:
- You can share objects initialized in the global session between test cases.
- No need to define local variables by yourself just for sharing them between test cases.
Avoid creating local variables in a test spec is good for ensuring the locality of variables inside your test methods.
In AirSpec you usually don't need to initialize/discard such shared variables in beforeAll/afterAll methods.


## Glocal and Local Sessions

AirSpec manages two types of sessions: _global_ and _local_:
- For each AirSpec instance, a single global session will be created.
- For each test method in the AirSpec instance, a local (child) session that inherits the global session will be created.

To configure the design of objects that will be created in each session,
override `configure(Design)` and `configureLocal(Design)` methods in AirSpec.

### Session LifeCycle

- Create a new instance of AirSpec
  - Run `beforeAll`
  - Call configure(design) to prepare a new global design
  - Start a new global session
     - for each test method _m_:
       - `before`
       - Call configureLocal(design) to prepare a new local design
       - Start a new local session
          - Call the test method _m_ by building method arguments using the local session (and the global session). See also [Airframe DI: Child Sessions](https://wvlet.org/airframe/docs/airframe.html#child-sessions) to learn more about the dependency resolution order.
       - Shutdown the local session. All data in the local session will be discarded
       - `after`
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
