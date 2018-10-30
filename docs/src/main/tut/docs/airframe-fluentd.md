--- 
layout: docs
title: airframe-fluentd
---

# Airframe Fluentd

airframe-fluentd is a logging library for sending object-based metrics to [Fluentd](https://www.fluentd.org/).

- Internally it uses [Fluency](https://github.com/komamitsu/fluency) as the fluentd client.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-fluentd_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-fluentd_2.12/)


## Usage

__build.sbt__
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-fluentd" % "(version)"

// For Scala.js (Since airframe-log 1.2)
libraryDependencies += "org.wvlet.airframe" %%% "airframe-fluentd" % "(version)"
```

### Example

```scala
import wvlet.airframe.fluentd._

case class MyMetric(a:Int, b:String)

val d = fluentd.withFluency // Using Fluency as the fluentd client
   // This line is unnecessary if you are using the default fluentd configuration
  .bind[FluentdConfig].toInstance(FluentdConfig(host="localhost", port=24224))
  // Set a common fluentd metric tag prefix (default = "")
  .bind[FluentdTag].toInstance(FluentdTag(prefix="")) 

d.build[MetricLoggerFactory] { f =>
   // Create a metric logger for MyMetric class
   val l = f.newMetricLogger[MyMetric](tag = "my_metric")
   l.emit(MyMetric(1, "hello"))   // my_metric {"a":1, "b":"hello"}
   l.emit(MyMetric(2, "fluentd")) // my_metric {"a":2, "b":"fluentd"}
}
```

### Debugging

For debugging purpose, use `fluentd.withConsoleLogging` design:

```scala
val d = fluentd.withConsoleLogging // Use a console logger instead of sending logs to Fluentd

d.build[MetricLoggerFactory] { f =>
   // Create a metric logger for MyMetric class
   val l = f.newMetricLogger[MyMetric](tag = "my_metric")
   l.emit(MyMetric(1, "hello"))   // prints [my_metric] {"a":1, "b":"hello"}
   l.emit(MyMetric(2, "fluentd")) // prints [my_metric] {"a":2, "b":"fluentd"}
}
```

This is convenient if you have no fluentd process running in your machine. 
