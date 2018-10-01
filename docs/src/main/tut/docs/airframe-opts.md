---
layout: docs
title: airframe-opts
---

# Airframe Opts

*airframe-opts* is a command-line parser library.

## Usage
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-opts_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-opts_2.12/)

**build.sbt**

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-opts" % "(version)"
```

### Define a command-line interface 
```scala
import wvlet.airframe.opts._
import wvlet.log.LogSupport

// Define a global option
case class GlobalOption(
  @option(prefix = "-h,--help", description = "display help messages", isHelp = true) 
  help: Boolean = false,
  @option(prefix = "-l,--loglevel", description = "log level") 
  loglevel: Option[LogLevel] = None
)

class MyApp(g:GlobalOption) extends DefaultCommand with LogSupport {
  Logger.setDefaultLogLevel(g.loglevel)

  def default {
    println("Type --help to display the list of commands")
  }

  @command(description = "say hello")
  def hello = {
    println("hello")
  }

  @command(description = "say world")
  def world(@argument message: String) {
    println(s"world ${message}")
  }
  
  @command(description = "start a server")
  def start(
    @option(prefix="-p,--port", description = "port number")
    port:Int = 8080,
    @option(prefix="--host",description = "server address")
    host:Option[String] = None
  ) {
    val addr = host.getOrElse("localhost")
    println(s"Starting server at ${addr}:${port}")
  }
}
```

### Launch a program 

```scala
package org.mydomain

object MyApp {
  def main(args:Array[String]) {
    val l = Launcher.of[MyApp]
    l.execute(args)
  }
}
```

### Packaging with sbt-pack
If you use [sbt-pack](https://github.com/xerial/sbt-pack) plugin, you can create a stand-alone Scala program with a command-line interface:

```scala
enablePlugins(PackPlugin)

// This example creates `myapp` command (target/pack/bin/hello) that calls org.mydomain.MyApp#main(Array[String]) 
packMain := Map("myapp" -> "org.mydomain.MyApp")
```


### Run the program 
```sh
$ sbt pack
...

$ ./target/pack/bin/myapp 
Type --help to display the list of commands
$ ./target/pack/bin/myapp --help
usage: [command name]

[options]
 -h, --help                 display help messages
 -l, --loglevel=[LOGLEVEL]  log level 
[commands]
 hello      say hello
 world     	say world

$ ./target/pack/bin/myapp hello
hello
```
