---
id: airframe-config
title: "airframe-config: Application Config Flow"
---

*airframe-config* enables configuring your Scala applications in a simple flow:

1. Write config classes of your application.
1. Read YAML files to populate the config objects.
1. (optional) Override the configuration with Properties.
1. Read the configuration with `config.of[X]` or bind it with Airframe DI. 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-config_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-config_2.12/)

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-config" % "(version)"
```

## Detailed application configuration flow

Here is the details of the application configuration flow:

1. The application specifies an environment (e.g., `test`, `staging`, `production`, etc) and configuration file paths.
1. Read a configuration file (YAML) from the paths specified in `configpaths`.
   - The first found YAML file in the config paths will be used.
   - `config.registerFromYaml[A](yaml file)` will create an object `A` from the YAML data.
   - If the YAML file does not contain data for the target environment, it searches for `default` environment instead.
       - If `default` environment is also not found, the provided default object will be used (optional).
1. (optional) Provide additional configurations (e.g., confidential information such as password, apikey, etc.)
   - Read these configurations in a secure manner and create a `Properties` object.
   - Override your configurations with `config.overrideWithProperties(Properties)`.
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
default: &default
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

**Scala code**:
```scala
import wvlet.airframe.config.Config
import wvlet.airframe.surface.tag.@@

// Configuration classes can have default values
// Configuration class name convention: xxxxConfig (xxxx will be the prefix for properties file)
case class LogConfig(file:String, maxFiles:Int=100, maxSize:Int=10485760)
case class ServerConfig(host:String, port:Int, password:String)

// To use the same configuration class for different purposes, use type tag (@@ Tag)
trait Access
trait Db

val config = 
  Config(env="development", configPaths=Seq("./config"))
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


## Using with Airframe

Since Airframe 0.65, binding configurations to design becomes easier.
By importing `wvlet.airframe.config._`, you can bind configurations to design objects.

Example:
```scala
import wvlet.airframe._
// Import config._ to use bindConfigXXX methods
import wvlet.airframe.config._

// Load "production" configurations from Yaml files
val design = 
  newDesign
    // Set an environment to use
    .withConfigEnv(env = "production", defaultEnv = "default")
    // Load configs from YAML files
    .bindConfigFromYaml[LogConfig]("access-log.yml")  
    .bindConfigFromYaml[ServerConfig]("server.yml")
    // Bind other designs
    .bind[X].toInstance(...)
    .bind[Y].toProvider{ ... }

// If you need to override some config parameters, prepare Map[String, Any] objects:
val properties = Map(
  "server.host" -> "xxx.xxx.xxx" 
)

// Override config with property values
val finalDesign = 
  design.overrideConfigParams(properties) 

finalDesign.build[X] { x =>
  // ... start the application 
}
```
