// Reload build.sbt on changes
Global / onChangedBuildSource := ReloadOnSourceChanges

val AIRFRAME_VERSION = sys.env.getOrElse("AIRFRAME_VERSION", "22.11.1")
val AIRSPEC_VERSION  = "22.11.1"
val SCALA_2_12       = "2.12.17"

ThisBuild / organization := "org.wvlet.airframe"

// Use dynamic snapshot version strings for non tagged versions
ThisBuild / dynverSonatypeSnapshots := true
// Use coursier friendly version separator
ThisBuild / dynverSeparator := "-"
ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

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
    "-deprecation"
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
        "io.get-coursier"    %% "coursier"              % "2.0.16",
        "org.apache.commons"  % "commons-compress"      % "1.22",
        "org.wvlet.airframe" %% "airframe-control"      % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-codec"        % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-log"          % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-http-codegen" % AIRFRAME_VERSION % Test
      ),
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq(
            "-Xmx1024M",
            "-Dplugin.version=" + version.value,
            "-Dairframe.version=" + AIRFRAME_VERSION
          )
      },
      scriptedBufferLog := false
    )
