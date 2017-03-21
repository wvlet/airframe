import ReleaseTransformations._
import sbt.Keys.mappings
import sbt._

val SCALA_2_12 = "2.12.1"
val SCALA_2_11 = "2.11.8"

scalaVersion in Global := SCALA_2_12

val buildSettings = Seq[Setting[_]](
  scalaVersion := SCALA_2_12,
  crossScalaVersions := Seq(SCALA_2_12, SCALA_2_11),
  organization := "org.wvlet",
  crossPaths := true,
  publishMavenStyle := true,
  // For performance testing, ensure each test run one-by-one
  concurrentRestrictions in Global := Seq(Tags.limit(Tags.Test, 1)),
  scalacOptions ++= Seq("-feature", "-deprecation"),
  logBuffered in Test := false,
  incOptions := incOptions.value
                .withNameHashing(true)
                // Suppress macro recompile warning: https://github.com/sbt/sbt/issues/2654
                .withLogRecompileOnMacro(false),
  updateOptions := updateOptions.value.withCachedResolution(true),
  sonatypeProfileName := "org.wvlet",
  pomExtra := {
  <url>https://github.com/xerial/wvlet</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/wvlet/log.git</connection>
      <developerConnection>scm:git:git@github.com:wvlet/log.git</developerConnection>
      <url>github.com/wvlet/wvlet.git</url>
    </scm>
    <developers>
      <developer>
        <id>leo</id>
        <name>Taro L. Saito</name>
        <url>http://xerial.org/leo</url>
      </developer>
    </developers>
  },
  // Use sonatype resolvers
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  // Release settings
  releaseCrossBuild := true,
  releaseTagName := { (version in ThisBuild).value },
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
    pushChanges
  )
)

lazy val root = Project(id = "root", base = file("."))
  .settings(
    buildSettings,
    publishArtifact := false,
    publish := {},
    publishLocal := {}
) aggregate(logJVM, logJS)

lazy val log =
  crossProject
  .in(file("wvlet-log"))
  .settings(buildSettings)
  .settings(
    name := "wvlet-log",
    description := "Fancy logger for Scala",    
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scalatest" %%% "scalatest" % "3.0.1" % "test"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-core" % "1.1.7"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.xerial.thirdparty.org_scala-js" %%% "scalajs-java-logging" % "0.1.1-pre1"
    )
  )

lazy val logJVM = log.jvm
lazy val logJS = log.js
