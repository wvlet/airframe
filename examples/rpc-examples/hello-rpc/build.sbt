val AIRFRAME_VERSION = "22.11.1"
ThisBuild / scalaVersion := "2.13.10"

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
      airframeHttpClients         := Seq("greeter.api:sync", "greeter.api:grpc"),
      airframeHttpGeneratorOption := "-l debug",
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-finagle" % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-http-grpc"    % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-launcher"     % AIRFRAME_VERSION
      )
    )
    .dependsOn(api)
