AirSpec
======

[AirSpec](https://github.com/wvlet/airframe/tree/master/airspec) is a new functional testing framework for Scala and Scala.js.

AirSpec uses pure Scala functions for writing test cases. This style requires no extra learning cost if you already know Scala. For advanced users, dependency injection to test cases and property-based testing are supported optionally.

- [GitHub: AirSpec](https://github.com/wvlet/airframe/tree/master/airspec)
- [Documentation](https://wvlet.org/airframe/docs/airspec)


## For Developers


```scala
$ ../sbt

// Publish a local snapshot of AirSpec for Scala 2.12, 2.13
> publishAllLocal

// Publish a snapshot of AirSpec for Scala 2.12, 2.13 to Sonatype
> publishSnapshot

// Building individual projects
> airspecJVM/compile
> airspecJVM/test
> airspecJVM/publishLocal

> airspecJS/compile
> airspecJS/test
> airspecJS/publishLocal
```

For Scala 3, launch sbt with DOTTY=true environment variable:

```scala
$ DOTTY=true ../sbt
```


Scals.js + Scala 3 version is not natively built, but you can use AirSpec by importing it with `.cross(CrossVersion.for3Use2_13)`:

```scala
libraryDependencies +=
 ("org.wvlet.airframe" %% "airspec" % AIRSPEC_VERSION).cross(CrossVersion.for3Use2_13)
```
