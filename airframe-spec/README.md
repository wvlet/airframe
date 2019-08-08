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

In AirSpec test cases are defined as functions in a class (or object) extending `AirSpec`.
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


## Writing Specs In Natural Languages

If you prefer natural language descriptions for your test cases, use symbols for class names or function names:

```scala
import wvlet.airframe.spec._

class `In A Set` extends AirSpec {
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
