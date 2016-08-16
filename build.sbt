import ReleaseTransformations._


val buildSettings = Seq[Setting[_]](
  scalaVersion := "2.11.8",
  organization := "org.wvlet",
  description := "Dependency injection library tailored to Scala",
  crossPaths := true,
  publishMavenStyle := true,
  // For performance testing, ensure each test run one-by-one
  concurrentRestrictions in Global := Seq(Tags.limit(Tags.Test, 1)),
  incOptions := incOptions.value.withNameHashing(true),
  logBuffered in Test := false,
  updateOptions := updateOptions.value.withCachedResolution(true),
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

lazy val wvletInject =
  Project(id = "airframe", base = file(".")).settings(
    buildSettings,
    description := "Dependency injection library tailored to Scala",
    libraryDependencies ++= Seq(
      "org.wvlet" % "wvlet-obj_2.11" % "0.16",
      "org.wvlet" % "wvlet-log_2.11" % "0.16",
      "org.wvlet" % "wvlet-test_2.11" % "0.16" % "test"
    )
  )