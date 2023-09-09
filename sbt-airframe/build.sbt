// Reload build.sbt on changes
Global / onChangedBuildSource := ReloadOnSourceChanges

val AIRFRAME_VERSION = sys.env.getOrElse("AIRFRAME_VERSION", "23.8.6")
val AIRSPEC_VERSION  = sys.env.getOrElse("AIRSPEC_VERSION", IO.read(file("../AIRSPEC_VERSION")).trim)
val SCALA_2_12       = "2.12.18"

ThisBuild / organization := "org.wvlet.airframe"

// Use dynamic snapshot version strings for non tagged versions
ThisBuild / dynverSonatypeSnapshots := true
// Use coursier friendly version separator
ThisBuild / dynverSeparator := "-"
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

val buildSettings = Seq[Setting[_]](
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://wvlet.org/airframe")),
  scmInfo := Some(
    ScmInfo(
      browseUrl = url("https://github.com/wvlet/airframe"),
      connection = "scm:git@github.com:wvlet/airframe.git"
    )
  ),
  developers := List(
    Developer(id = "leo", name = "Taro L. Saito", email = "leo@xerial.org", url = url("http://xerial.org/leo"))
  ),
  sonatypeProfileName := "org.wvlet",
  crossPaths          := true,
  publishMavenStyle   := true,
  publishTo           := sonatypePublishToBundle.value,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    // For using import * syntax
    "-Xsource:3"
  ),
  testFrameworks += new TestFramework("wvlet.airspec.Framework"),
  libraryDependencies ++= Seq(
    "org.wvlet.airframe" %% "airspec" % AIRSPEC_VERSION % Test
  )
)

// sbt plugin
lazy val sbtAirframe =
  project
    .in(file("."))
    .enablePlugins(SbtPlugin, BuildInfoPlugin)
    .settings(
      buildSettings,
      buildInfoKeys ++= Seq[BuildInfoKey](
        name,
        version,
        "airframeVersion" -> AIRFRAME_VERSION,
        scalaVersion,
        sbtVersion
      ),
      buildInfoPackage := "wvlet.airframe.sbt",
      name             := "sbt-airframe",
      description      := "sbt plugin for helping programming with Airframe",
      scalaVersion     := SCALA_2_12,
      libraryDependencies ++= Seq(
        "io.get-coursier"    %% "coursier"              % "2.1.6",
        "org.apache.commons"  % "commons-compress"      % "1.24.0",
        "org.wvlet.airframe" %% "airframe-control"      % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-codec"        % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-log"          % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-http-codegen" % AIRFRAME_VERSION % Test
      ),
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq(
            "-Xmx1024M",
            s"-Dplugin.version=${version.value}",
            s"-Dairframe.version=${AIRFRAME_VERSION}",
            s"-Dairspec.version=${AIRSPEC_VERSION}"
          )
      },
      scriptedBufferLog := false
    )
