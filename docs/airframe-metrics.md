---
id: airframe-metrics
title: "airframe-metrics: Human-Friendly Measures for Time and Data Size"
---

*airframe-metrics* is a library for human-readable representations of time, data byte size, etc.


## Usage
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-surface_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-metrics_2.12/)

**build.sbt**

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-metrics" % "(version)"
```


## DataSize

Human-readable byte-size representation (e.g., 10GB, 1TB, etc.)

```scala
import wvlet.airframe.metrics.DataSize

// Returns the most succinct representation of the data size
DataSize.succinct(1024).toString // 1kB
DataSize.succinct(3 * 1024 * 1024).toString // 3MB

// Parse the datasize strings
DataSize("5GB") // DataSize(5.0, GIGABYTE)
```

## ElapsedTime

Human-readable time representation (e.g., 1ns, 1 us, 1ms, 1s, 1m, 1h, 1d, etc.) useful for
measuring elapsed times.

```scala
import wvlet.airframe.metrics.ElapsedTime

// Returns the most succinct
ElapsedTime.succinctMillis(3 * 1000) // 3.00s
ElapsedTime.succinctMillis(60 * 1000) // 1.00m

// Parse the elapsed time strings
val e = ElapsedTime("1h") // ElapsedTime(1.0, HOURS)
e.toMillis // 3600000
```

## Count

Human-readable integer representation for Long (signed 64-bit integer) values.

```scala
import wvlet.airframe.metrics.Count

Count.succinct(123L).toString // 123
Count.succinct(1234L).toString // 1.23K
Count.succinct(1234567L).toString // 1.23M

Count("1.23M") // Count(1230000L, Count.MILLION)
Count("1,234") // Count(1234, Count.ONE)
```

## Relative Time Windows

When writing data processing programs of time-series data, we usually don't care about the exact date. Rather we use *relative* time windows from the current time, such as today, last 7 days, last 6 hours, last month, etc.
`TimeWindow` class in airframe-metrics enables specifying such time windows using a human-friendly string format.

### Code Example
```scala
import wvlet.airframe.metrics.TimeWindow

val w =
  TimeWindow
  .withTimeZone("PDT")  // Set a time zone (you can also use withSystemTimeZone, withUTC, etc.)
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
// seconds, minutes, hours, days, weeks, (M)onths, quarters, years
UNIT     := s | m | h | d | w | M | q | y

OFFSET   := DURATION | DATE_TIME
RANGE    := (OFFSET) (/ (OFFSET))*
DATE_TIME := yyyy-MM-dd( HH:mm:ss(.ZZZ|' ' z)?)?
```

### Examples

Here are several examples of relative time window strings when the current time is `2016-06-26 01:23:45-0700`:

 Duration| Definition | start      | end (exclusive) |
---------|------------|------------|-----------------|
   `1h`    | this hour  | `2016-06-26 01:00:00-0700` | `2016-06-26 02:00:00-0700` |
   `1d`    | today | `2016-06-26 00:00:00-0700` | `2016-06-27 00:00:00-0700` |
   `1M`    | this month | `2016-06-01 00:00:00-0700` | `2016-07-01 00:00:00-0700` |
  `-1h`   | last hour   |  `2016-06-26 00:00:00-0700` | `2016-06-26 01:00:00-0700`|
  `-1h/0h`   | last hour   |  `2016-06-26 00:00:00-0700` | `2016-06-26 01:00:00-0700`|
  `-1h/0m`   | last hour until last minute |  `2016-06-26 00:00:00-0700` | `2016-06-26 01:23:00-0700`|
  `-1h/0s`   | last hour until last second |  `2016-06-26 00:00:00-0700` | `2016-06-26 01:23:45-0700`|
  `-1h/now`   | last hour to now  |  `2016-06-26 00:00:00-0700` | `2016-06-26 01:23:45-0700`|
`-60m/2017-01-23 01:23:45`| last 60 min from an offset | `2017-01-23 00:23:00-0700` | `2017-01-23 01:23:00-0700`|
  `-1d`    | last day   |  `2016-06-25 00:00:00-0700` | `2016-06-26 00:00:00-0700`|
  `-7d`    | last 7 days | `2016-06-19 00:00:00-0700` | `2016-06-26 00:00:00-0700`|
 `-7d/now` | last 7 days to now | `2016-06-10 00:00:00-0700` | `2016-06-26 01:23:45-0700`|
`-3d/2017-04-07`| last 3 days from an offset | `2017-04-04 00:00:00-0700` | `2017-04-07 00:00:00-0700`|
`+7d` | next 7 days (including today) | `2016-06-26 00:00:00-0700` | `2016-07-03 00:00:00-0700`|
`+7d/now`| next 7 days from now | `2016-06-26 01:23:45-0700` | `2016-07-03 00:00:00-0700`|
  `-1w`    | last week  |`2016-06-13 00:00:00-0700` | `2016-06-20 00:00:00-0700`|
  `-1M`    | last month |`2016-05-01 00:00:00-0700` | `2016-06-01 00:00:00-0700`|
  `-1q`    | last quarter |`2016-01-01 00:00:00-0700` | `2016-04-01 00:00:00-0700`|
  `-1y`    | last year  |`2015-01-01 00:00:00-0700` | `2016-01-01 00:00:00-0700`|
`-1h/2017-01-23 01:00:00`| last hour from offset (hour) | `2017-01-23 00:00:00-0700` | `2017-01-23 01:00:00-0700`|
`-1h/2017-01-23 01:23:45`| last hour from offset (hour) | `2017-01-23 00:00:00-0700` | `2017-01-23 01:00:00-0700`|
`-1M/2017-01-23 01:23:45`| last month from offset (hour) | `2016-12-01 00:00:00-0700` | `2017-01-01 00:00:00-0700`|
`2017-01-23/2017-01-25`| exact date range | `2017-01-23 00:00:00-0700` | `2017-01-25 00:00:00-0700`|
`2017-01-23 01:23:45/2017-01-25 01:23:45`| exact time range | `2017-01-23 01:23:45-0700` | `2017-01-25 1:23:45-0700`|
`2016-01-01/0d`| from time to the offset | `2016-01-01 00:00:00-0700` | `2016-06-26 00:00:00-0700`|
`0M/2017-01-23)`| no unit truncation (backward)| `2017-01-01 00:00:00-0700` | `2017-01-23 00:00:00-0700`|
`+1M/2017-01-23 01:23:45)`| no unit truncation (forward)| `2017-01-23 01:23:45-0700` | `2017-02-01 00:00:00-0700`|
