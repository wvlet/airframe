val AIRFRAME_VERSION = "24.2.0"
ThisBuild / scalaVersion := "3.2.2"

// RPC API definition. This project should contain only RPC interfaces
lazy val api =
  project
    .in(file("api"))
    .settings(
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http" % AIRFRAME_VERSION
      )
    )

lazy val greeter =
  project
    .in(file("."))
    .enablePlugins(AirframeHttpPlugin, PackPlugin)
    .settings(
      // Generates HTTP clients
      airframeHttpClients         := Seq("greeter.api:rpc:GreeterRPC"),
      airframeHttpGeneratorOption := "-l debug",
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-netty" % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-launcher"   % AIRFRAME_VERSION
      )
    )
    .dependsOn(api)
