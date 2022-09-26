AirSpec
======

[AirSpec](https://github.com/wvlet/airframe/tree/master/airspec) is a new functional testing framework for Scala and Scala.js.

AirSpec uses pure Scala functions for writing test cases. This style requires no extra learning cost if you already know Scala. For advanced users, dependency injection to test cases and property-based testing are supported optionally.

- [GitHub: AirSpec](https://github.com/wvlet/airframe/tree/master/airspec)
- [Documentation](https://wvlet.org/airframe/docs/airspec)


## For Developers


```scala
$ ../sbt

// Publish a local snapshot of AirSpec for Scala 2.12, 2.13, and Scala 3
> publishAllLocal

// Publish a snapshot of AirSpec for Scala 2.12, 2.13, and Scala 3 to Sonatype
> publishSnapshots

// Building individual projects
> airspecJVM/compile
> airspecJVM/test
> airspecJVM/publishLocal

> airspecJS/compile
> airspecJS/test
> airspecJS/publishLocal
```

For Scala 3, select Scala 3 compiler

```scala
$ ../sbt
# Select Scala 3
> ++ 3
```

Scals.js + Scala 3 version is build natively, you can use AirSpec by importing it without using `.cross(CrossVersion.for3Use2_13)`:

```scala
# Scala 3 + Scala.js
libraryDependencies += "org.wvlet.airframe" %%% "airspec" % AIRSPEC_VERSION % Test
```
