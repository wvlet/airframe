airframe-control
====

airframe-control is a library for writing control flow at ease.


## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-control_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-control_2.12/)

__build.sbt__
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-control" % "(version)"
```

### Control

```scala
import wvlet.airframe.control.Control

// Loan pattern
Control.withResource(new FileInputStream("in.txt")){ in =>
  ...
}

// Handle two resources
Control.withResources(
  new FileInputStream("in.txt"), new FileOutputStream("out.txt")
){ (in, out) =>
  ...
}
```

### Retry


```scala
import wvlet.airframe.control.Retry
import java.util.concurrent.TimeoutException

// Backoff retry
val r: String =
  Retry
    .withBackOff(maxRetry = 3)
    .retryOn { 
       case e: TimeoutException => Retry.retryableFailure(e)
    }
    .run {
      logger.info("hello retry")
      if (count < 2) {
        count += 1
        throw new TimeoutException("retry test")
      } else {
        "success"
      }
    }
```

### Parallel


```scala
import wvlet.airframe.control.Parallel

// Simply run a given function for each element of the source collection
val source: Seq[Int] = Seq(1, 2, 3)
val result: Seq[Int] = Parallel.run(source, parallelism = 4){ i =>
  ...
}

// `Iterator` can be used instead of `Seq` as a source. This version is useful to handle a very large data.
val source: Iterator[Int] = ...
val result: Iterator[Int] = Parallel.iterate(source, parallelism = 4){ i =>
  ...
}

```

or

```scala
import wvlet.airframe.control.parallel._

// This syntax works for both Seq and Iterator
val result = source.parallel.withParallelism(4).map { i =>
  ...
}
```

You can monitor metrics of parallel execution via JMX using [airframe-jmx](https://github.com/wvlet/airframe/tree/master/airframe-jmx).

```
JMXAgent.defaultAgent.register[Parallel.ParallelExecutionStats](Parallel.jmxStats)
```

