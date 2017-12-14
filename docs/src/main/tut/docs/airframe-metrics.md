---
layout: docs
title: airframe-metrics
---

# Airframe Metrics

*airframe-metrics* is a library for using human-readable representations of time, data byte size, etc.


## Usage
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-surface_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-metrics_2.12/)

**build.sbt**

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-metrics" % "(version)"
```

## Relative Time Window

When writing data processing programs of time-series data, we usually don't care about the exact date. Rather we use *relative* time windows from the current time, such as today, last 7 days, last 6 hours, last month, etc.
`TimeWindow` class in airframe-metrics enables specifying such time windows using a human-friendly string format.

### Code Example
```scala
import wvlet.airframe.metrics.TimeWindow

val w = 
  TimeWindow
  .withTimeZone("PDT")  // Set a time zone (or you can use withSystemTimeZone, withUTC, etc.)
  .withOffset("2016-06-26 01:23:45-0700") // Specify the context time. The default is the current time.

// last 7 days
println(w.parse("-7d"))     // [2016-06-19 00:00:00-0700,2016-06-26 00:00:00-0700)

// last 7 days to now 
println(w.parse("-7d/now")) // [2016-06-19 00:00:00-0700,2016-06-26 01:23:45-0700) 
```

## Time Window String Representation

To specify time ranges at ease, airframe-metrics uses the following string representation of time windows, such as `-1d`, `-30m`, `+7d`, etc.
Values with (or without)`-` sign means the *last* time range, and values with `+` sign means the next time range from the context time.

### Format


```
DURATION := (+ | -)?(INTEGER)(UNIT)
// seconds, minutes, hours, days, weeks, (M)onths, years
UNIT     := s | m | h | d | w | M | y

OFFSET   := DURATION | DATE_TIME
RANGE    := (DURATION) (/ (OFFSET))?
DATE_TIME := yyyy-MM-dd( HH:mm:ss(.ZZZ|' ' z)?)?
```

### Examples

Supporse the current time is `2016-06-26 01:23:45-0700`.

| Duration| Definition           | Time Range |
|---------|------------|------------|
|   `0h`    | this hour  | `[2016-06-26 01:00:00-0700,2016-06-26 02:00:00-0700)` | 
|   `0d`    | today | `[2016-06-26 00:00:00-0700,2016-06-27 00:00:00-0700)` | 
|   `0M`    | this month | `[2016-06-01 00:00:00-0700,2016-07-01 00:00:00-0700)` | 
|  `1h`   | last hour   |  `[2016-06-26 00:00:00-0700,2016-06-26 01:00:00-0700)`|         
|  `-1h`   | last hour   |  `[2016-06-26 00:00:00-0700,2016-06-26 01:00:00-0700)`|         
|  `-1h/now`   | last hour to now  |  `[2016-06-26 00:00:00-0700,2016-06-26 01:23:45-0700)`|
|`60m/2017-01-23 01:23:45`| last 60 minutes to now| `[2017-01-23 00:23:00-0700,2017-01-23 01:23:45-0700)`|
|  `-1d`    | last day   |  `[2016-06-25 00:00:00-0700,2016-06-26 00:00:00-0700)`| 
|  `-7d`    | last 7 days | `[2016-06-19 00:00:00-0700,2016-06-26 00:00:00-0700)`| 
|  `7d`    | last 7 days | `[2016-06-19 00:00:00-0700,2016-06-26 00:00:00-0700)`| 
| `-7d/now` | last 7 days to now | `[2016-06-10 00:00:00-0700,2016-06-26 01:23:45-0700)`|
|`-3d/2017-04-07`| last 3 days from a given offset | `[2017-04-04 00:00:00-0700,2017-04-07 00:00:00-0700)`|
|`+7d` | next 7 days (including today) | `[2016-06-26 00:00:00-0700,2016-07-03 00:00:00-0700)`|
|`+7d/now`| next 7 days from now | `[2016-06-26 01:23:45-0700,2016-07-03 00:00:00-0700)`|
|  `-1w`    | last week  |`[2016-06-13 00:00:00-0700,2016-06-20 00:00:00-0700)`|
|  `-1M`    | last month |`[2016-05-01 00:00:00-0700,2016-06-01 00:00:00-0700)`|
|  `-1y`    | last year  |`[2015-01-01 00:00:00-0700,2016-01-01 00:00:00-0700)`|
|`-1h/2017-01-23 01:00:00`| last hour to a given offset | `[2017-01-23 00:00:00-0700,2017-01-23 01:00:00-0700)`|
|`-1h/2017-01-23 01:23:45`| last hour to a given offset | `[2017-01-23 00:00:00-0700,2017-01-23 01:23:45-0700)`|


## Other Classes

- DataSize 
  - Human-redable byte-size representation (e.g., 10GB, 1TB, etc.)
- ElapsedTime
  - Human-readable time representation (e.g., 1s, 1m, 1d, etc.)  

