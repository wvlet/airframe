---
id: airframe-launcher
title: airframe-launcher: Command-Line Program Launcher
---

*airframe-laucnher* is a handy command-line program launcher. 

## Usage
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-launcher_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-launcher_2.12/)

**build.sbt**

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-launcher" % "(version)"
```

## Quick Start

1. Import `wvlet.airframe.launcher._`
1. Create a class `A` with constructor arguments annotated with `@option` or `@argument`
1. Call `Launcher.of[A].execute("command line")` 

```scala
import wvlet.airframe.launcher._

class MyApp(@option(prefix = "-h,--help", description = "display help messages", isHelp = true) 
            help: Boolean = false,
            @option(prefix = "-p", description = "port number") 
            port: Int = 8080) {

   @command(isDefault = true)
   def default: Unit = {
     println(s"Hello airframe. port:${port}")
   }
}
```

A function annotated with `@defaultCommand` will be executed.

```scala
Launcher.execute[MyApp]("-p 10010")
// Hello airframe. port:10010

Launcher.execute[MyApp]("")
// Hello airframe. port:8080
```

If there is a _help_ option (`isHelp = true`), command line help messages can be generated automatically.

For example, enable the help option like this:
```scala
Launcher.execute[MyApp]("--help")
```

Then it will show the following help message:
```
usage: myapp [options]

[options]
 -p [PORT]   port number
 -h, --help  show help messages
```

### Available Annotations

- `@option` options 
  - You can specify multiple option prefixes (e.g., `-h,--help`) for the same option
- `@argument`
  - For mapping non-option arguments. If you want to handle multiple arguments, use `Seq[String]`, `Array[String]` types.
- `@command`
  - Defining function or class as a command module. You can specify `description` and (one-line) `usage` of the command in this annotation.
  - If no sub command name is given, a function annotated with `@command(isDefault = true)`  will be executed as the default command.

## Defining Multiple Sub Commands

You can define a command module (= a set of sub commands) using functions in a class.

Add `@command` annotation to functions in order to define sub commands:
```scala
import wvlet.airframe.launcher._
import wvlet.log.LogSupport

// Define a global option
case class GlobalOption(
  @option(prefix = "-h,--help", description = "display help messages", isHelp = true) 
  help: Boolean = false,
  @option(prefix = "-l,--loglevel", description = "log level") 
  loglevel: Option[LogLevel] = None
)

class MyApp(g:GlobalOption) extends LogSupport {
  Logger.setDefaultLogLevel(g.loglevel)

  @command(isDefault = true)
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

You can call sub commands by specifying the name of the function.
The sub command name will be matched in a canonical form (ignoring case and removing any symbols like '_', '-')

```scala
Launcher.execute[MyApp]("hello")
// hello

Launcher.execute[MyApp]("start --host localhost -p 10000")
// Straing server at localhost:10000
```

## Nesting Sub Commands

With `addModule`, you can add nested command set

```scala
// Add sub commands `command1` and `command2`:
val l = Launcher.of[A]
  .addModule[B](name="command1", description="nested command set")
  .addModule[C](name="command2", description="...")

// Launch B
l.execute("command1")

// Launch C
l.execute("command2")
```

It is also possible to nest Launcher(s):

```scala
val subModule = Launcher.of[A]

val l = Launcher.of[Main]
  .add(subModule, name="cmd-a", description="sub command set")

// Launch A in subModule
l.exucute("cmd-a")
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
usage: [options] <command name>

[options]
 -h, --help                 display help messages
 -l, --loglevel=[LOGLEVEL]  log level 
[commands]
 hello      say hello
 world     	say world

$ ./target/pack/bin/myapp hello
hello
```
