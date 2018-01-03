import sbtcrossproject.{crossProject, CrossType}

val SCALA_2_13          = "2.13.0-M2"
val SCALA_2_12          = "2.12.4"
val SCALA_2_11          = "2.11.11"
val targetScalaVersions = Seq(SCALA_2_13, SCALA_2_12, SCALA_2_11)

val SCALATEST_VERSION   = "3.0.4"
val SQLITE_JDBC_VERSION = "3.21.0.1"

// For using Scala 2.12 in sbt
scalaVersion in ThisBuild := SCALA_2_12

organization in ThisBuild := "org.wvlet.airframe"

val isTravisBuild: Boolean = sys.env.isDefinedAt("TRAVIS")
// In release process, this environment variable should be set
val isRelease: Boolean = sys.env.isDefinedAt("RELEASE")

def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val prefix = out.ref.dropV.value
  val rev    = out.commitSuffix.mkString("+", "-", "")
  val dirty  = out.dirtySuffix.value
  val dynamicVersion = (rev, dirty) match {
    case ("", "") =>
      prefix
    case (_, _) =>
      // (version)+(distance)-(rev)
      s"${prefix}${rev}"
  }
  if (isRelease) dynamicVersion else s"${dynamicVersion}-SNAPSHOT"
}

def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer timestamp d}"

inThisBuild(
  List(
    version := dynverGitDescribeOutput.value.mkVersion(versionFmt, fallbackVersion(dynverCurrentDate.value)),
    dynver := {
      val d = new java.util.Date
      sbtdynver.DynVer.getGitDescribeOutput(d).mkVersion(versionFmt, fallbackVersion(d))
    }
  ))

// For publishing in Travis CI
lazy val travisSettings = List(
  // For publishing on Travis CI
  useGpg := false,
  usePgpKeyHex("42575E0CCD6BA16A"),
  pgpPublicRing := file("./travis/local.pubring.asc"),
  pgpSecretRing := file("./travis/local.secring.asc"),
  // PGP_PASS, SONATYPE_USER, SONATYPE_PASS are encoded in .travis.yml
  pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray),
  credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    sys.env.getOrElse("SONATYPE_USER", ""),
    sys.env.getOrElse("SONATYPE_PASS", "")
  )
)

inThisBuild(if (isTravisBuild) travisSettings else List.empty)

val buildSettings = Seq[Setting[_]](
  scalaVersion := SCALA_2_12,
  crossScalaVersions := targetScalaVersions,
  crossPaths := true,
  publishMavenStyle := true,
  logBuffered in Test := false,
  scalacOptions ++= Seq("-feature", "-deprecation"),
  sonatypeProfileName := "org.wvlet",
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/wvlet/airframe")),
  scmInfo := Some(
    ScmInfo(
      browseUrl = url("https://github.com/wvlet/airframe"),
      connection = "scm:git@github.com:wvlet/airframe.git"
    )
  ),
  developers := List(
    Developer(id = "leo", name = "Taro L. Saito", email = "leo@xerial.org", url = url("http://xerial.org/leo"))
  ),
  // Use sonatype resolvers
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  publishTo := Some(
    if (isRelease)
      Opts.resolver.sonatypeStaging
    else
      Opts.resolver.sonatypeSnapshots
  )
)

val jsBuildSettings = Seq[Setting[_]](
  // Skip Scala 2.11 + Scala.js build
  crossScalaVersions := Seq(SCALA_2_12),
  // Do not run tests concurrently
  concurrentRestrictions in Global := Seq(
    Tags.limit(Tags.Test, 1)
  )
  // Workaround for ' JSCom has been closed' issue
  //parallelExecution in ThisBuild := false
)

val noPublish = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

lazy val root =
  project
    .in(file("."))
    .settings(name := "airframe-root")
    .settings(buildSettings)
    .settings(noPublish)
    .aggregate(projectJVM, projectJS)

lazy val projectJVM =
  project.settings(noPublish).aggregate(airframeJVM, surfaceJVM, logJVM, airframeSpecJVM, config, jmx, opts, metricsJVM, codec, tablet, jdbc)

lazy val projectJS =
  project.settings(noPublish).aggregate(airframeJS, surfaceJS, logJS, metricsJS, airframeSpecJS)

lazy val docs =
  project
    .in(file("docs"))
    .settings(
      name := "docs",
      publishArtifact := false,
      publish := {},
      publishLocal := {},
      // Necessary for publishMicrosite
      git.remoteRepo := "git@github.com:wvlet/airframe.git",
      ghpagesNoJekyll := false,
      micrositeName := "Airframe",
      micrositeDescription := "Libraries for Building Full-Fledged Scala Applications",
      micrositeAuthor := "Taro L. Saito",
      micrositeOrganizationHomepage := "https://github.com/wvlet",
      micrositeHighlightTheme := "ocean",
      micrositeGithubOwner := "wvlet",
      micrositeGithubRepo := "airframe",
      micrositeBaseUrl := "airframe",
      micrositeAnalyticsToken := "UA-98364158-1",
      micrositeDocumentationUrl := "docs",
      //micrositePushSiteWith := GitHub4s,
      //micrositeGithubToken := sys.env.get("GITHUB_REPO_TOKEN"),
      micrositePalette ++= Map(
        "brand-primary"   -> "#2582AA",
        "brand-secondary" -> "#143F56",
        "brand-tertiary"  -> "#042F46",
        "gray-dark"       -> "#453E46",
        "gray"            -> "#534F54"
      ),
      watchSources += new sbt.internal.io.Source(
        sourceDirectory.value,
        new FileFilter {
          def accept(f: File) = !f.isDirectory
        },
        NothingFilter
      )
    )
    .enablePlugins(MicrositesPlugin)

def parallelCollection(scalaVersion: String) = {
  if (scalaVersion.startsWith("2.13.")) {
    Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "0.1.2")
  } else {
    Seq.empty
  }
}

lazy val airframe =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe"))
    .settings(buildSettings)
    .settings(
      name := "airframe",
      description := "Dependency injection library tailored to Scala",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      )
    )
    .jvmSettings(
      // include the macro classes and resources in the main jar
      mappings in (Compile, packageBin) ++= mappings.in(airframeMacrosJVM, Compile, packageBin).value,
      // include the macro sources in the main source jar
      mappings in (Compile, packageSrc) ++= mappings.in(airframeMacrosJVM, Compile, packageSrc).value
    )
    .jsSettings(
      jsBuildSettings,
      // Copy macro classes into the main jar
      mappings in (Compile, packageBin) ++= mappings.in(airframeMacrosJS, Compile, packageBin).value.filter(x => x._2 != "JS_DEPENDENCIES"),
      // include the macro sources in the main source jar
      mappings in (Compile, packageSrc) ++= mappings.in(airframeMacrosJS, Compile, packageSrc).value
    )
    .dependsOn(surface, airframeMacros % "compile-internal,test-internal", airframeSpec % "test")

lazy val airframeJVM = airframe.jvm
lazy val airframeJS  = airframe.js

// Airframe depends on Airframe Macros, so we needed to split the project
lazy val airframeMacros =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe-macros"))
    .settings(buildSettings)
    .settings(
      buildSettings,
      name := "airframe-macros",
      description := "Macros for Airframe",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      ),
      publish := {},
      publishLocal := {}
    )
    .jsSettings(jsBuildSettings)

lazy val airframeMacrosJVM = airframeMacros.jvm
lazy val airframeMacrosJS  = airframeMacros.js

lazy val surface =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe-surface"))
    .settings(buildSettings)
    .settings(
      name := "airframe-surface",
      description := "A library for extracting object structure surface",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
        "org.scalatest"  %%% "scalatest"    % SCALATEST_VERSION % "test"
      )
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log, airframeSpec % "test")

lazy val surfaceJVM = surface.jvm
lazy val surfaceJS  = surface.js

lazy val config =
  project
    .in(file("airframe-config"))
    .settings(buildSettings)
    .settings(
      name := "airframe-config",
      description := "airframe configuration module",
      libraryDependencies ++= Seq(
        "org.yaml" % "snakeyaml" % "1.18"
      )
    )
    .dependsOn(surfaceJVM, tablet, airframeSpecJVM % "test")

lazy val jmx =
  project
    .in(file("airframe-jmx"))
    .settings(buildSettings)
    .settings(
      name := "airframe-jmx",
      description := "A library for exposing Scala object data through JMX"
    )
    .dependsOn(surfaceJVM, airframeSpecJVM % "test")

lazy val opts =
  project
    .in(file("airframe-opts"))
    .settings(buildSettings)
    .settings(
      name := "airframe-opts",
      description := "Command-line option parser",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6"
      )
    )
    .dependsOn(surfaceJVM, airframeSpecJVM % "test")

// airframe-log should have minimum dependencies
lazy val log =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe-log"))
    .settings(buildSettings)
    .settings(
      name := "airframe-log",
      description := "Fancy logger for Scala",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
        "org.scalatest"  %%% "scalatest"   % SCALATEST_VERSION  % "test"
      ) ++ parallelCollection(scalaVersion.value)
    )
    .jvmSettings(
      libraryDependencies ++= Seq("ch.qos.logback" % "logback-core" % "1.2.3")
    )
    .jsSettings(
      jsBuildSettings,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-java-logging" % "0.1.3"
      )
    )

lazy val logJVM = log.jvm
lazy val logJS  = log.js

lazy val metrics =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe-metrics"))
    .settings(buildSettings)
    .settings(
      name := "airframe-metrics",
      description := "Basit metric representations, including duration, size, time window, etc.",
      libraryDependencies ++= Seq()
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log, airframeSpec % "test")

lazy val metricsJVM = metrics.jvm
lazy val metricsJS  = metrics.js

lazy val airframeSpec =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe-spec"))
    .settings(buildSettings)
    .settings(
      name := "airframe-spec",
      description := "Airframe spec test base library",
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % SCALATEST_VERSION
      ) ++ parallelCollection(scalaVersion.value)
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log)

lazy val airframeSpecJVM = airframeSpec.jvm
lazy val airframeSpecJS  = airframeSpec.js

lazy val codec =
  project
    .in(file("airframe-codec"))
    .settings(buildSettings)
    .settings(
      name := "airframe-codec",
      description := "Airframe MessagePack-based codec",
      libraryDependencies ++= Seq(
        "org.msgpack"    % "msgpack-core" % "0.8.14",
        "org.scalacheck" %% "scalacheck"  % "1.13.5" % "test"
      )
    )
    .dependsOn(logJVM, surfaceJVM, airframeSpecJVM % "test")

lazy val tablet =
  project
    .in(file("airframe-tablet"))
    .settings(buildSettings)
    .settings(
      name := "airframe-tablet",
      description := "Data format conversion library",
      libraryDependencies ++= Seq(
        // scala-csv doesn't support Scala 2.13 yet
        // "com.github.tototoshi" %% "scala-csv"   % "1.3.5",
        // For ColumnType parser and JSON parser
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
        "org.scalacheck"         %% "scalacheck"               % "1.13.5" % "test",
        // For JDBC testing
        "org.xerial" % "sqlite-jdbc" % SQLITE_JDBC_VERSION % "test"
      )
    )
    .dependsOn(codec, logJVM, surfaceJVM, airframeSpecJVM % "test")

lazy val jdbc =
  project
    .in(file("airframe-jdbc"))
    .settings(buildSettings)
    .settings(
      name := "airframe-jdbc",
      description := "JDBC connection pool service",
      libraryDependencies ++= Seq(
        "org.xerial"     % "sqlite-jdbc" % SQLITE_JDBC_VERSION,
        "org.postgresql" % "postgresql"  % "42.1.4",
        "com.zaxxer"     % "HikariCP"    % "2.6.2",
        // For routing slf4j log to airframe-log
        "org.slf4j" % "slf4j-jdk14" % "1.7.25"
      )
    )
    .dependsOn(airframeJVM, airframeMacrosJVM % "provided", airframeSpecJVM % "test")
