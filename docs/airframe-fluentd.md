---
id: airframe-fluentd
title: airframe-fluentd: Fluentd Logger
---

airframe-fluentd is a logging library for sending metrics to [Fluentd](https://www.fluentd.org/) or 
[Treasure Data](https://www.treasuredata.com/)

- Internally it uses [Fluency](https://github.com/komamitsu/fluency) as the fluentd client.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-fluentd_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-fluentd_2.12/)

## Usage

__build.sbt__
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-fluentd" % "(version)"

# If you need to emit logs to Treasure Data, add this dependency as well:
libraryDependencies +=  "org.komamitsu" % "fluency-treasuredata" % "2.4.1"
```

### Sending Data to Fluentd

```scala
import wvlet.airframe.fluentd._

// Define a metric class
case class MyMetric(a:Int, b:String) extends TaggedMetric {
  // Used for defining the default tag prefix for this metric.
  // (tagPrefix).(metricTag) will be used as fluentd tag. 
  override def metricTag: String = "my_metric"
}

// Creating a logger to use the local fluentd (host="localhost", port=24224)
// [optional] tagPrefix: common tag prefix for all metrics  
val d = fluentd.withFluendLogger(tagPrefix = "data")

d.build[MetricLoggerFactory] { f =>
   // Create a metric logger for MyMetric class
   val l = f.getTypedLogger[MyMetric]

   l.emit(MyMetric(1, "hello"))   // data.my_metric {"a":1, "b":"hello"}
   l.emit(MyMetric(2, "fluentd")) // data.my_metric {"a":2, "b":"fluentd"}
}
```

### Sending Data to Treasure Data

```Scala
import wvlet.airframe.fluentd._

// Define a metric class
case class MyMetric(a:Int, b:String) extends TaggedMetric {
  // Specify the table name to store this metric
  override def metricTag: String = "my_metric"
}

// Creating a logger to send log data to Treasure Data Stream Import API:
val d = fluentd.withTDLogger(apikey = "(Your TD API key)",
  tagPrefix = "(database name to store logs)"
)

d.build[MetricLoggerFactory] { f =>
   // Create a metric logger for MyMetric class
   val l = f.getTypedLogger[MyMetric]

   // Metrics will be stored in data.my_mertric table
   l.emit(MyMetric(1, "hello"))   // data.my_metric {"a":1, "b":"hello"}
   l.emit(MyMetric(2, "fluentd")) // data.my_metric {"a":2, "b":"fluentd"}
}
```

### Using Non-Typed Logger

```scala
val d = fluentd.withFluendLogger()

d.build[MetricLoggerFactory] { f =>
   val l = f.getLogger
   l.emit("data.my_metric", Map("a"->1, "b"->"hello"))
}
```

## Debugging Metrics

For debugging purpose, use `fluentd.withConsoleLogging` design:

```scala
val d = fluentd.withConsoleLogging // Use a console logger instead of sending logs to Fluentd

d.build[MetricLoggerFactory] { f =>
   val l = f.getTypedLogger[MyMetric]
   l.emit(MyMetric(1, "hello"))   // prints data.my_metric: {"a":1, "b":"hello"}
   l.emit(MyMetric(2, "fluentd")) // prints data.my_metric: {"a":2, "b":"fluentd"}
}
```


