airframe-spec
=== 

airframe-spec is a base trait for writing tests using ScalaTest.

With airframe-spec, you can: 
- Configuring loggers automatically 
- Reload log levels periodically using log-test.properties file in `src/test/resources` folder.

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-jmx_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-spec_2.12/)

**build.sbt**
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-spec" % "(version)" % "test"
```


```scala
package org.yourdomain
import wvlet.airframe.AirframeSpec

class MyTest extends AirframeSpec {
  "my test" in {
    // ... write your test here  
  }
}
```

To configure log-level while running tests, add `log-test.properties`:

**src/test/resources/log-test.properties**
```
org.yourdomain=debug
```

This sets debug loglevel to classes under `org.yourdomain` package. It is recommended to add `log-test.properties` 
to .gitignore as as not to share such configurations for debugging.
