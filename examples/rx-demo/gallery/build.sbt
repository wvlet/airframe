Global / onChangedBuildSource := ReloadOnSourceChanges

val AIRFRAME_VERSION = "2026.1.1"
ThisBuild / scalaVersion := "2.13.18"

lazy val gallery =
  project
    .enablePlugins(ScalaJSPlugin)
    .in(file("."))
    .settings(
      scalaJSUseMainModuleInitializer := false,
      scalacOptions ++= Seq(
        // Necessary for tracking source code range in airframe-rx demo
        "-Yrangepos"
      ),
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %%% "airframe-rx-html" % AIRFRAME_VERSION
      )
    )
