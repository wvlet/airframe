AirSpec (airframe-spec)
======

[AirSpec](https://wvlet.org/airframe/airframe-spec) is a lightweight testing framework for Scala and Scala.js.

# Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-spec_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-spec_2.12/)

**build.sbt**

```
libraryDependencies += "org.wvlet.airframe" %% "airframe-spec" % "(version)"
testFrameworks += new TestFramework("wvlet.airframe.spec.AirSpecFramework")
```

For Scala.js, use `%%%`:
```
libraryDependencies += "org.wvlet.airframe" %%% "airframe-spec" % "(version)"
```

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

## Running Tests

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
