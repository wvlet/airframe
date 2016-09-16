import ReleaseTransformations._

val buildSettings = Seq[Setting[_]](
  scalaVersion := "2.11.8",
  organization := "org.wvlet",

  crossPaths := true,
  publishMavenStyle := true,
  // For performance testing, ensure each test run one-by-one
  concurrentRestrictions in Global := Seq(Tags.limit(Tags.Test, 1)),
  incOptions := incOptions.value.withNameHashing(true),
  logBuffered in Test := false,
  updateOptions := updateOptions.value.withCachedResolution(true),
  scalacOptions ++= Seq("-feature"),
  sonatypeProfileName := "org.wvlet",
  pomExtra := {
  <url>https://github.com/wvlet/airframe</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
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
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value

(compile in Compile) <<= (compile in Compile) dependsOn compileScalastyle

val WVLET_VERSION="0.26"

lazy val airframeRoot = Project(id="airframe-root", base = file(".")).settings(
  buildSettings,
  publishArtifact := false,
  publish := {},
  publishLocal := {}
) aggregate(airframe, airframeMacros)

lazy val airframe = Project(id = "airframe", base = file("airframe")).settings(
  buildSettings,
  description := "Dependency injection library tailored to Scala",
  libraryDependencies ++= Seq(
    "org.wvlet" %% "wvlet-obj" % WVLET_VERSION,
    "org.wvlet" %% "wvlet-log" % "1.0",
    "org.scalatest" %% "scalatest" % "2.2.+" % "test",
    "org.scalacheck" %% "scalacheck" % "1.11.4" % "test"
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
