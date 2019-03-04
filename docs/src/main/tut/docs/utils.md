---
layout: docs
title: Airframe Utilities
---

# Airframe Utilities

Airframe has several utilities that make easier your programming in Scala:

- [airframe](index.html)
  - Scala-friendly dependency injection library.
- [ariframe-canvas](airframe-canvas.html)
  - Off-heap memory buffer
- [airframe-codec](airframe-codec.html)
  - [MessagePack](https://msgpack.org) based Schema-on-Read data transcoder 
- [airframe-config](airframe-config.html)
  - Configuration reader & provider.
- [airframe-control](airframe-control.html)
  - Utilities for controlling code flows with loan pattern, retry logic, parallelization, etc.
- [airframe-fluentd](airframe-fluentd.html)
  - MetricLogger for sending logs to fluentd or Treasure Data
- [airframe-http](airframe-http.html)
  - A light-weight HTTP server builder
- [airframe-http-recorder](airframe-http-recorder.html)
  - A handly HTTP recorder and replayer for HTTP server development
- [airframe-jdbc](airframe-jdbc.html)
  - Reusable JDBC connection pool.
- [airframe-jmx](airframe-jmx.html)
  - Enable application monitoring through JMX.
- [airframe-json](airframe-json.html)
  - Pure-Scala JSON parser.
- [airframe-log](airframe-log.html)
  - Light-weight handy logging library.
- [airframe-launcher](airframe-launcher.html)
  - Command line parser and launcher.
- [airframe-metrics](airframe-metrics.html)
  - Human-readable representation of times, time ranges, and data sizes.
- [airframe-msgpack](airframe-msgpack.html)
  - Pure-scala MessagePack reader and writer
- [airframe-surface](airframe-surface.html)
  - Object shape inspector. What parameters are defined in an object? Surface gives you an answer for that. 
- [airframe-spec](airframe-spec.html)
  - A simple base trait for using ScalaTest.
- [airframe-tablet](airframe-tablet.html)
  - Table-structured data (e.g., CSV, TSV, JDBC ResultSet) reader/writer.

## sbt-plugins

- [sbt-pack](https://github.com/xerial/sbt-pack)
  - A sbt plugin for creating distributable Scala packages.
  - Packaging jar dependencies into a folder 
  - You can also [build a docker image of your program](https://github.com/xerial/sbt-pack#building-a-docker-image-file-with-sbt-pack).
  
- [sbt-sonatype](https://github.com/xerial/sbt-sonatype)
  - A sbt plugin for publishing Scala/Java projects to the Maven central.
  - Enables [a single command release](https://github.com/xerial/sbt-sonatype#using-with-sbt-release-plugin) of your project.

