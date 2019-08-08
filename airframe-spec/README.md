AirSpec
======

[AirSpec](https://wvlet.org/airframe/airframe-spec) is a lightweight testing framework for Scala and Scala.js.

# Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-spec_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-spec_2.12/)

**build.sbt**

```
libraryDependencies += "org.wvlet.airframe" %% "airframe-spec" % "(version)"
testFrameworks += new TestFramework("wvlet.airframe.spec.runnger.AirSpecFramework")
```

For Scala.js:
```
libraryDependencies += "org.wvlet.airframe" %%% "airframe-spec" % "(version)"
```

## Writing Unit Tests 

Extends `AirSpec` in a class, and public method in this class will be executed as test cases.  
```scala
import wvlet.airframe.spec._

class MyTest extends AirSpec {
  // Basic assertion
  def emptySeqSizeShouldBe0: Unit = {
    assert(Seq.empty.size == 0)
  }
  
  // Catch an exception
  def emptySeqHeadShouldFail {
    intercept[NoSuchElementException] {
      Seq.empty.head
    }
  }
}

```
