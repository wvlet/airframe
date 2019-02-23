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

# If you need to emit logs to Treasure Data, add this dependeency as well:
libraryDependencies +=  "org.komamitsu" % "fluency-treasuredata" % "2.0.0"
```

### Example

```scala
import wvlet.airframe.fluentd._

case class MyMetric(a:Int, b:String)

// Using Fluency as the fluentd client
val d = fluentd.withFluency()
  // Set a common fluentd metric tag prefix (default = "")
  .bind[FluentdTag].toInstance(FluentdTag(prefix="data")) 

d.build[MetricLoggerFactory] { f =>
   // Create a metric logger for MyMetric class
   val l = f.newMetricLogger[MyMetric](tag = "my_metric")
   l.emit(MyMetric(1, "hello"))   // data.my_metric {"a":1, "b":"hello"}
   l.emit(MyMetric(2, "fluentd")) // data.my_metric {"a":2, "b":"fluentd"}
}
```


## Customizing Fluency 

### Debugging

For debugging purpose, use `fluentd.withConsoleLogging` design:

```scala
val d = fluentd.withConsoleLogging // Use a console logger instead of sending logs to Fluentd

d.build[MetricLoggerFactory] { f =>
   // Create a metric logger for MyMetric class
   val l = f.newMetricLogger[MyMetric](tag = "my_metric")
   l.emit(MyMetric(1, "hello"))   // prints data.my_metric: {"a":1, "b":"hello"}
   l.emit(MyMetric(2, "fluentd")) // prints data.my_metric: {"a":2, "b":"fluentd"}
}
```

This is convenient if you have no fluentd process running in your machine. 
