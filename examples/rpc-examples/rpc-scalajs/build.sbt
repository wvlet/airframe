Global / onChangedBuildSource := ReloadOnSourceChanges

val AIRFRAME_VERSION = "21.10.0"
ThisBuild / scalaVersion := "2.13.7"

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
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-finagle" % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-launcher"     % AIRFRAME_VERSION
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
      airframeHttpClients             := Seq("example.api:scalajs"),
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %%% "airframe-rx-html" % AIRFRAME_VERSION
      )
    )
    .dependsOn(apiJS)
