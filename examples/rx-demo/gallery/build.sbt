Global / onChangedBuildSource := ReloadOnSourceChanges

val AIRFRAME_VERSION = "2026.2.2"
ThisBuild / scalaVersion := "3.3.7"

lazy val gallery =
  project
    .enablePlugins(ScalaJSPlugin)
    .in(file("."))
    .settings(
      scalaJSUseMainModuleInitializer := false,
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-rx-html" % AIRFRAME_VERSION
      )
    )
