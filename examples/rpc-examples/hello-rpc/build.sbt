val AIRFRAME_VERSION = "20.9.2"
scalaVersion in ThisBuild := "2.12.12"

// RPC API definition. This project should contain only RPC interfaces
lazy val greeter =
  project
    .in(file("."))
    .enablePlugins(AirframeHttpPlugin, PackPlugin)
    .settings(
      // Generates HTTP clients
      airframeHttpClients := Seq("greeter.api:sync", "greeter.api:grpc"),
      airframeHttpGeneratorOption := "-l debug",
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http"         % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-http-finagle" % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-http-grpc"    % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-launcher"     % AIRFRAME_VERSION
      )
    )
