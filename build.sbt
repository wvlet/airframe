import ReleaseTransformations._

scalaVersion := "2.12.1"

val buildSettings = Seq[Setting[_]](
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.12.1", "2.11.8"),
  organization := "org.wvlet",
  crossPaths := true,
  publishMavenStyle := true,
  // For performance testing, ensure each test run one-by-one
  concurrentRestrictions in Global := Seq(Tags.limit(Tags.Test, 1)),
  incOptions := incOptions.value
                .withNameHashing(true)
                // Suppress macro recompile warning: https://github.com/sbt/sbt/issues/2654
                .withLogRecompileOnMacro(false),
  logBuffered in Test := false,
  updateOptions := updateOptions.value.withCachedResolution(true),
  scalacOptions ++= Seq("-feature", "-deprecation"),
  sonatypeProfileName := "org.wvlet",
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/wvlet/airframe")),
  pomExtra := {
    <scm>
      <connection>scm:git:github.com/wvlet/airframe.git</connection>
      <developerConnection>scm:git:git@github.com:wvlet/airframe.git</developerConnection>
      <url>github.com/wvlet/airframe.git</url>
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
  ),
  releaseCrossBuild := true
)

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value

(compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value


lazy val airframeRoot = Project(id="airframe-root", base = file(".")).settings(
  buildSettings,
  publishArtifact := false,
  publish := {},
  publishLocal := {}
) aggregate(airframe, airframeMacros, surface)

lazy val airframe = Project(id = "airframe", base = file("airframe")).settings(
  buildSettings,
  description := "Dependency injection library tailored to Scala",
  libraryDependencies ++= Seq(
    "org.wvlet" %% "object-schema" % "1.0",
    "org.wvlet" %% "wvlet-log" % "1.1",
    // scalatest
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "org.scalacheck" %% "scalacheck" % "1.12.6" % "test"
  ),
  // include the macro classes and resources in the main jar
  mappings in (Compile, packageBin) ++= mappings.in(airframeMacros, Compile, packageBin).value,
  // include the macro sources in the main source jar
  mappings in (Compile, packageSrc) ++= mappings.in(airframeMacros, Compile, packageSrc).value
) dependsOn(airframeMacros % "provided")

lazy val airframeMacros = Project(id = "airframe-macros", base = file("airframe-macros")).settings(
  buildSettings,
  description := "Macros for Airframe",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )
)

lazy val surface = Project(id = "surface", base = file("surface")).settings(
  buildSettings,
  description := "Object Surface is a library for extracting object structure",
  libraryDependencies ++= Seq(
    "org.wvlet" %% "wvlet-log" % "1.1",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    // scalatest
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "org.scalacheck" %% "scalacheck" % "1.12.6" % "test"
  )
)

