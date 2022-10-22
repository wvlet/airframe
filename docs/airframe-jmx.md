---
id: airframe-jmx
title: airframe-jmx: JMX Application Monitor
---

airframe-jmx enables exposing application information through JMX so that you can check the running state of an application outside JVM. For example, you can
use `jconsole` program to access JMX parameters.

JMX already provides various JVM metrics (e.g., heap memory usage, GC statistics, etc.). DataDog provides a handy way to collect JMX metrics:

 * [Monitoring Java application through DataDog](http://docs.datadoghq.com/integrations/java/)

For analyzing application behavior for longer ranges (5 minute or more), we recommend using [Treasure Data](https://treasuredata.com) along with DataDog:
```
JMX -> fluentd -> DataDog   (For real-time monitoring)
               -> Treasure Data -> Presto SQL (Doing metric-driven actions with SQL queries)
```

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-jmx_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-jmx_2.12/)

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-jmx" % "(version)"
```

Usage is simple: Add `@JMX` annotation to variables or methods you want to see in JMX.


## Registering JMX parameters
```scala
import wvlet.airframe.jmx._

@JMX(description = "A example MBean object")
class SampleMBean {
  @JMX(description = "free memory size")
  def freeMemory: Long = {
    Runtime.getRuntime.freeMemory()
  }
}

// Register the MBean to make it visible from JMX interface
val mbean = new SampleMBean
JMXAgent.defaultAgent.register[SampleMBean](mbean)
```

## Nested parameters

To report nested parameters, add `@JMX` to parameters as well:
```scala
class NestedMBean {
  @JMX(description = "nested stat")
  def stat: Stat = {
    Stat(Random.nextInt(10), "nested JMX bean")
  }
}

case class Stat(@JMX count: Int, @JMX state: String)
```
In this example, `stat.count` and `stat.state` will be reported.


## Specify parameter names explicitly

Parameter names are automatically generated but also can be specified explicitly.
```scala
@JMX(description = "A example MBean object")
class NamedMBean {
  @JMX(name = "memory.free", description = "free memory size")
  def freeMemory: Long = {
    Runtime.getRuntime.freeMemory()
  }
}
```

## Launching JMX Registry

You can launch JMXRegistry (e.g., on port 7199) by setting these JVM parameters:
```
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=7199
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.local.only=false
-Dcom.sun.management.jmxremote.ssl=false
```

For convenience, you can start JMXRegistry inside your program:

```scala
wvlet.airframe.jmx.JMXAgent.start(registryPort=7199)
```

## Using airframe-jmx with Airframe DI

```scala
import wvlet.airframe._
import wvlet.airframe.jmx._

case class MyAppStats(
  @JMX(description = "application access count")
  accessCount:Int
)

@JMX(description = "My application")
trait MyApp { self =>

  bind[JMXAgent].onStart { agent =>
    // Register MyApp to JMX registry when starting the application
    agent.register[MyApp](self)
  }

  private var accessCount: Long = 0

  @JMX(description = "application stats")
  def stats: MyAppStats = {
    MyAppStats(accessCount)
  }
}

```
