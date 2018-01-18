---
layout: docs
title: Airframe Utilities
---

# Airframe Utilities

Airframe has several utilities that make easier your programming in Scala:
    
- [airframe-log](airframe-log.html) 
  - Light-weight handy logging library.
- [airframe-codec](airframe-codec.html) 
  - [MessagePack](https://msgpack.org) based Schema-on-Read data transcoder 
- [airframe-config](airframe-config.html)
  - Configuration reader & provider.
- [airframe-opts](airframe-opts.html) 
  - Command line parser.
- [airframe-metrics](airframe-metrics.html) 
  - Human-readable representation of time, time range, and data size.
- [airframe-jdbc](airframe-jdbc.html) 
  - Reusable JDBC connection pool implementation.
- [airframe-jmx](airframe-jmx.html) 
  - Enable application monitoring through JMX.
- [airframe-surface](airframe-surface.html) 
  - Object shape inspector. What parameters are defined in an object? Surface gives you an answer for that. 
- [airframe-spec](https://github.com/wvlet/airframe/blob/master/airframe-spec/shared/src/main/scala/wvlet/airframe/AirframeSpec.scala) 
  - A simple base trait for using ScalaTest.

## sbt-plugins

- [sbt-pack](https://github.com/xerial/sbt-pack)
  - A sbt plugin for creating distributable Scala packages.
  - Packaging jar dependencies into a folder 
  - You can also [build a docker image of your program](https://github.com/xerial/sbt-pack#building-a-docker-image-file-with-sbt-pack).
  
- [sbt-sonatype](https://github.com/xerial/sbt-sonatype)
  - A sbt plugin for publishing Scala/Java projects to the Maven central.
  - Enables [a single command release](https://github.com/xerial/sbt-sonatype#using-with-sbt-release-plugin) of your project.

