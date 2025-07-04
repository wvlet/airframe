// Reload build.sbt on changes
Global / onChangedBuildSource := ReloadOnSourceChanges

val AIRFRAME_VERSION = sys.env.getOrElse("AIRFRAME_VERSION", "24.12.1")
val AIRSPEC_VERSION  = sys.env.getOrElse("AIRSPEC_VERSION", "24.12.1")
val SCALA_2_12       = "2.12.20"

ThisBuild / organization := "org.wvlet.airframe"

// Use dynamic snapshot version strings for non tagged versions
ThisBuild / dynverSonatypeSnapshots := true
// Use coursier friendly version separator
ThisBuild / dynverSeparator := "-"
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

val buildSettings = Seq[Setting[?]](
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
  crossPaths        := true,
  publishMavenStyle := true,
  javacOptions ++= Seq("-source", "11", "-target", "11"),
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
        "io.get-coursier"    %% "coursier"              % "2.1.24",
        "org.apache.commons"  % "commons-compress"      % "1.27.1",
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
