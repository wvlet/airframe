# sbt-airframe plugin

A sbt plugin for generating [Airframe RPC](https://wvlet.org/airframe/docs/airframe-rpc) clients

sbt-airframe plugins supports generating HTTP clients for making RPC calls. sbt-airframe supports
generating async, sync, or Scala.js HTTP clients.

[![maven central](https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22
)

Add the following plugin settings:

__project/plugins.sbt__

```scala
addSbtPlugin("org.wvlet.airframe" % "sbt-airframe" % "(version)")
```

To generate HTTP clients, add `airframeHttpClients` setting to your `build.sbt`. You need to specify
which API package to use for generating RPC clients. The format
is `<RPC package name>:<client type>(:<target package name>(.<target class name>)?)?`. For example:

__build.sbt__

```scala
enablePlugins(AirframeHttpPlugin)

airframeHttpClients := Seq("hello.api.v1:sync")

// [optional] Specify airframe-http version to use
aifframeHttpVersion := (AIRFRAME_VERSION)
```

Supported client types are:

- __sync__: Create a sync HTTP client (ServiceSyncClient) for Scala (JVM)
- __async__: Create an async HTTP client (ServiceClient) for Scala (JVM) using Future
  abstraction (`F`). The `F` can be `scala.concurrent.Future` or twitter-util's Future.
- __scalajs__:  Create an RPC client (ServiceClientJS)
- __grpc__: Create gRPC client factory (ServiceGrpc: SyncClient, AsyncClient)

To support other types of clients, see the examples
of [HTTP code generators](https://github.com/wvlet/airframe/blob/master/airframe-http-codegen/src/main/scala/wvlet/airframe/http/codegen/client/ScalaHttpClientGenerator.scala)
. This code reads a Router definition of RPC interfaces, and generate client code for calling RPC
endpoints. Currently, we only supports generating HTTP clients for Scala. In near future, we would
like to add Open API spec generator so that many programming languages can be used with Airframe
RPC.

The generated client code can be found in `target/scala-2.12/src_managed/(api package)/` folder.

#### sbt-airframe commands

```scala
# Regenerate the generated client code.Use this if RPC interface has changed
> airframeHttpReload

# Generating RPC clients manually
> airframeHttpGenerateClients

# Clean the generated code
> airframeHttpClean
```

### Open API

sbt-airframe plugin also supports generating [Open API](http://spec.openapis.org/oas/v3.0.3)
specification from Airframe RPC interfaces. To generate OpenAPI spec from RPC definition,
add `airframeHttpOpenAPIPackages` configuration to your build.sbt:

```scala
// [Required] RPC packages to use for generating Open API specification
airframeHttpOpenAPIPackages := Seq("hello.api")
// [Optional] Specify target directory to generate openapi.yaml. The default is target directory
airframeHttpOpenAPITargetDir := target.value
// [Optional] Additional configurations (e.g., title, version, etc.)
airframeHttpOpenAPIConfig := OpenAPIConfig(
  title = "My API", // default is project name
  version = "1.0.0", // default is project version,
  format = "yaml", // yaml (default) or json
  filePrefix = "openapi" // Output file name: (filePrefix).(format)
)
```

With this configuration, Open API spec will be generated when running `package` task:

```scala
> package
```

It will generate `target/openapi.yaml` file.
