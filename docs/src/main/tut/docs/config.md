--- 
layout: docs
title: Configuring Applications
---

# Airframe Config

*airframe-config* enables configuring your Scala applications in a simple flow:

1. Write config classes of your application.
1. Read YAML files to populate the config objects.
1. (optional) Override the configuration with Properties.
1. Use it!

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/airframe-config_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-config_2.12/)

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-config" % "(version)"
```

## Detailed application configuration flow

Here is the details of the application configuration flow:

1. The application specifies an environment (e.g., `test`, `staging`, `production`, etc) and configuration file paths.
1. Read a configuration file (YAML) from `configpath(s)`.
   - The first found YAML file in the config paths will be used.
   - `config.registerFromYaml[A](yaml file)` will create an object `A` from the YAML data.
   - If the YAML file does not contain data for the target environment, it searches for `default` environment instead.
       - If `default` environment is also not found, the provided default object will be used (optional).
1. (optional) Provide additional configurations (e.g., confidential information such as password, apikey, etc.)
   - Read these configurations in a secure manner and create a `Properties` object.
   - Override your configurations with `config.overideWithPropertie(Properties)`.
1. Get the configuration with `config.of[A]`


## Example

**config/access-log.yml**:
```
default:
  file: log/access.log
  max_files: 50
```

**config/db-log.yml**:
```
default:
  file: log/db.log
```

**config/server.yml**
```
default:
  host: localhost
  port: 8080

# Override the port number for development
development:
  <<: *default
  port: 9000
```

**config.properties**:
```
# [prefix](@[tag])?.[param]=(value)
log@access.file=/path/to/access.log
log@db.file=/path/to/db.log
log@db.log.max_files=250
server.host=mydomain.com
server.password=xxxxxyyyyyy
```

**Sacla code**:
```scala
import wvlet.config.Config
import wvlet.surface.tag.@@

// Configuration classes can have default values
// Configuration class name convention: xxxxConfig (xxxx will be the prefix for properties file)
case class LogConfig(file:String, maxFiles:Int=100, maxSize:Int=10485760)
case class ServerConfig(host:String, port:Int, password:String)

// To use the same configuration class for different purposes, use type tag (@@ Tag)
trait Access
trait Db

val config = 
  Config(env="development", configPaths="./config")
    .registerFromYaml[LogConfig @@ Access]("access-log.yml")
    .registerFromYaml[LogConfig @@ Db]("db-log.yml")
    .registerFromYaml[ServerConfig]("server.yml")
    .overrideWithPropertiesFile("config.properties")
    
val accessLogConfig = config.of[LogConfig @@ Access]
// LogConfig("/path/to/access.log",50,104857600)

val dbLogConfig = config.of[LogConfig @@ Db]
// LogConfig("/path/to/db.log",250,104857600)

val serverConfig = config.of[ServerConfig]
// ServerConfig(host="mydomain.com",port=9000,password="xxxxxyyyyyy")

```


### Show configuration changes

To see the effective configurations, use `Config.getConfigChanges` method:
```scala
for(change <- config.getConfigChanges) {
  println(s"[${change.key}] default:${change.default}, current:${change.current}")
}
```


## Usign with Airframe

Here is an example of using `Config` with `Airframe`:

```scala
import wvlet.airframe._
import wvlet.config.Config

...

val env = "prodcution"

// For overriding properties, or you can load the environmental variables here
val properties = Map(
  "server.host" -> "xxx.xxx.xxx" 
)

// Load "production" configurations from Yaml files
val config: Config =
  Config(env = env, defaultEnv = "default")
  .registerFromYaml[LogConfig]("access-log.yml")  
  .registerFromYaml[ServerConfig]("server.yml")
  .overrideWith(properties) // Override config with property values

// Bind config to design
val design = config.getAll.foldLeft(newDesign) {
  (d: Design, c: ConfigHolder) => d.bind(c.tpe).toInstance(c.value)
}
  
val firnalDesign = design
  .bind[X].toInstance(...)
  .bind[Y].toProvider{ ... }
   
val session = finalDesign.newSession
...

```
