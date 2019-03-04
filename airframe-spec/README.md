airframe-spec
=== 

airframe-spec is a base trait for writing tests using ScalaTest.

With airframe-spec, you can: 
- Configuring loggers automatically 
- Reload log levels periodically using log-test.properties file in `src/test/resources` folder.

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-jmx_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-spec_2.12/)

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-spec" % "(version)" % "test"
```


```scala
import wvlet.airframe.spec.AirframeSpec

class MyTest extends AirframeSpec {
  "my test" in {
    // ... write your test here  
  }
}
```
