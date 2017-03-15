import ReleaseTransformations._
import sbt.Keys.mappings
import sbt._

val buildSettings = Seq[Setting[_]](
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq(
    "2.11.8",
    "2.12.0"
  ),
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
) aggregate(logJVM, logJS, logMacrosJVM, logMacrosJS)

lazy val logMacros =
  crossProject
  .in(file("wvlet-log-macros"))
  .settings(buildSettings)
  .settings(
    name := "wvlet-log-macros",
    description := "Macros for wvlet-log",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
    )
  )

lazy val logMacrosJVM = logMacros.jvm
lazy val logMacrosJS = logMacros.js

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
  .dependsOn(logMacros)
  .jvmSettings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-core" % "1.1.7"
    ),
    // include the macro classes and resources in the main jar
    mappings in (Compile, packageBin) ++= mappings.in(logMacrosJVM, Compile, packageBin).value,
    // include the macro sources in the main source jar
    mappings in (Compile, packageSrc) ++= mappings.in(logMacrosJVM, Compile, packageSrc).value
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-java-logging" % "0.1.0"
    ),
    // include the macro classes and resources in the main jar
    mappings in (Compile, packageBin) ++= mappings.in(logMacrosJS, Compile, packageBin).value,
    // include the macro sources in the main source jar
    mappings in (Compile, packageSrc) ++= mappings.in(logMacrosJS, Compile, packageSrc).value
  )

lazy val logJVM = log.jvm
lazy val logJS = log.js
