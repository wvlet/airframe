# Airframe Control

airframe-control is a library for writing control flow easily.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-control_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-control_2.12/)

## Usage

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

// Backoff retry
val r: String =
  Retry
    .withBackOff(maxRetry = 3)
    .retryOn { s: RetryContext[_] =>
      warn(s"[${s.retryCount}/${s.maxRetry}] ${s.lastError.getMessage}. Retrying in ${s.nextWaitMillis} millis")
    }
    .run {
      logger.info("hello retry")
      if (count < 2) {
        count += 1
        throw new IllegalStateException("retry test")
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

// Run each element every 10 seconds
val stoppable = Parallel.repeat(source, interval = 10 seconds){ i =>
  ...
}

// Stop running
stoppable.stop()
```
