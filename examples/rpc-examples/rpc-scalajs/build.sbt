Global / onChangedBuildSource := ReloadOnSourceChanges

val AIRFRAME_VERSION = "2025.1.16"
ThisBuild / scalaVersion := "3.2.2"

lazy val rpcExample =
  project
    .in(file("."))
    .aggregate(apiJVM, apiJS, server, ui)

lazy val api =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("api"))
    .settings(
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %%% "airframe-http" % AIRFRAME_VERSION
      )
    )

lazy val apiJVM = api.jvm
lazy val apiJS  = api.js

lazy val server =
  project
    .in(file("server"))
    .settings(
      // For using the project root as a working folder
      reStart / baseDirectory := (ThisBuild / baseDirectory).value,
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-netty" % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-launcher"   % AIRFRAME_VERSION
      )
    )
    .dependsOn(apiJVM)

lazy val ui =
  project
    .enablePlugins(ScalaJSPlugin, AirframeHttpPlugin)
    .in(file("ui"))
    .settings(
      airframeHttpOpenAPIPackages     := Seq("example.api"),
      scalaJSUseMainModuleInitializer := true,
      airframeHttpClients             := Seq("example.api:rpc"),
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %%% "airframe-rx-html" % AIRFRAME_VERSION
      )
    )
    .dependsOn(apiJS)
