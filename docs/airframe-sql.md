---
id: airframe-sql
title: "airframe-sql: SQL Parser"
---

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-sql_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-sql_2.12/)
[![Latest version](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/airframe)

[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/wvlet?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge

airframe-sql is a SQL parser/lexer and model classes that follows SQL-92 standard.

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-sql" % "(version)"
```

## Usage
```scala
import wvlet.airframe.sql.parser.SQLParser

// Parse SQL and generate a logical plan 
val logicalPlan = SQLParser.parse("select * from a") // Project[*](Table("a")) 
```
