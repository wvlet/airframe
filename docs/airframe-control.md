---
id: airframe-control
title: airframe-control: Retry/Rate Control
---

airframe-control is a colleciton of libraries to manage control flows, that are especially useul for making remote API calls.
For example, airframe-control has exponential back-off retry, jittering, circuit breaker, parallel task execution support, etc.

- [Source Code at GitHub](https://github.com/wvlet/airframe/tree/master/airframe-control)

# Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-control_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-control_2.12/)

__build.sbt__
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-control" % "(version)"
```

## Control

This provides a handy Loan Pattern syntax for preperly open and close resources:

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

## Retry

### Exponential Backoff

Exponential backoff will multiply the waiting time for each retry attempt. The default multiplier is 1.5. For example, if the initial waiting time is 1 second, the next waiting time will be 1 x 1.5 = 1.5 second, and the next waiting time will be 1.5 * 1.5 = 2.25 seconds, and so on.

```scala
import wvlet.airframe.control.Retry
import java.util.concurrent.TimeoutException

// Backoff retry
val r: String =
  Retry
    .withBackOff(maxRetry = 3)
    // Classify the retryable or non-retryable error type. All exceptions will be retried by default.
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

To classify error types within `retryOn` method, use `Retry.retryableFailure(Throwable)` or `Retry.nonRetryableFailure(Throwable)`.


### Adding Extra Wait

```scala
import wvlet.airframe.control.Retry
import java.util.concurrent.TimeoutException

Retry
  .withJitter()
  .retryOn {
     case e: IllegalArgumentException =>
       Retry.nonRetryableFailure(e)
     case e: TimeoutException =>
       Retry
         .retryableFailure(e)
         // Add extra wait millis
         .withExtraWaitMillis(50)
  }
```

### Bounded Time Backoff

To decide the number of backoff retries from an expected total wait time, use `withBoundedBackoff`:
```scala
import wvlet.airframe.control.Retry

Retry
  .withBoundedBackoff(
    initialIntervalMillis = 1000,
    maxTotalWaitMillis = 30000
  )
```

### Jitter

Jitter is useful to add randomness between the retry intervals especially if there are multiple tasks using the same retry pattern. For example, if the base waiting time is 10 seconds, Jitter will pick a next waiting time between [0, 10] to add some random factor. Then, the base waiting time will be multiplied as in the exponential backoff. This randomness will avoid having multiple API calls that will be retried at the same timing, which often cause resource contention or overload of the target service. With Jittering you can avoid such unexpected correlations between retried requests.

```scala
import wvlet.airframe.control.Retry
import java.util.concurrent.TimeoutException

Retry
  .withJitter(maxRetry = 3) // It will wait nextWaitMillis * rand() upon retry
  .retryOn {
    case e: TimeoutException =>
      Retry.retryableFailure(e)
  }
  .run {
    // body
  }
```

## CircuitBreaker

CircuitBreaker is used to avoid excessive calls to a remote service when the service is unavailable, and provides the capability to fail-fast the application so that we can avoid adding an extra waiting time before getting any response from the struggling service.

CircuitBreaker is useful for:
- Adding a safety around remote API calls
- Protecting the system from too many exceptions of the same type.


CircuitBreaker has tree states: CLOSED, OPEN, and HALF_OPEN.

- __CLOSED__: This is the default state where all executions are allowed. If the target service becomes unhealthy (markedDead), the states will transit to OPEN state.
- __OPEN__: The connection to the target service is broken in this state, and no execution will be allowed. In this state, all executions will throw CircuitBreakerOpenException to perform fail-fast so that we can quickly return the control to the caller. After a certain amount of time is passed specified by delayAfterMarkedDead policy, this state will shift to HALF_OPEN state.
- __HALF_OPEN__: This state will perform a _probing_ to the target service. That means, an execution to the target service is allowed once, and if the request succeeds the state will move to CLOSED state. If the request fails, it will go back to OPEN state again. The delay interval time will be computed by some retry policy. The default delay policy is an exponential backoff (30 seconds initial wait) with jittering.

```scala
import wvlet.airframe.control.CircuitBreaker

val cb = CircuitBreaker
  .withFailureThreshold(3, 10) // Open the circuit when observing 3 failures out of 10 executions
  .run {
    // body
  }
```

## Parallel

Parallel is a library for ensuring using a fixed number of threads (= parallelism) for running tasks.


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

```scala
JMXAgent.defaultAgent.register[Parallel.ParallelExecutionStats](Parallel.jmxStats)
```

