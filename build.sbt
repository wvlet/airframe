import sbtcrossproject.{CrossType, crossProject}

val SCALA_2_11 = "2.11.12"
val SCALA_2_12 = "2.12.8"
val SCALA_2_13 = "2.13.0"

val SCALA_JS_VERSION = "0.6.28"

val untilScala2_12      = SCALA_2_12 :: SCALA_2_11 :: Nil
val targetScalaVersions = SCALA_2_13 :: untilScala2_12

val SCALATEST_VERSION               = "3.0.8"
val SCALACHECK_VERSION              = "1.14.0"
val MSGPACK_VERSION                 = "0.8.16"
val SCALA_PARSER_COMBINATOR_VERSION = "1.1.2"
val SQLITE_JDBC_VERSION             = "3.27.2"
val SLF4J_VERSION                   = "1.7.25"
val JS_JAVA_LOGGING_VERSION         = "0.1.5"
val airSpecFramework                = new TestFramework("wvlet.airframe.spec.AirSpecFramework")

// Allow using Ctrl+C in sbt without exiting the prompt
cancelable in Global := true

// For using Scala 2.12 in sbt
scalaVersion in ThisBuild := SCALA_2_12
organization in ThisBuild := "org.wvlet.airframe"

val isTravisBuild: Boolean = sys.env.isDefinedAt("TRAVIS")
// In release process, this environment variable should be set
val isRelease: Boolean = sys.env.isDefinedAt("RELEASE")

// Use dynamic snapshot version strings for non tagged versions
dynverSonatypeSnapshots in ThisBuild := !isRelease

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

inThisBuild(
  if (isTravisBuild) travisSettings
  else List.empty)

val buildSettings = Seq[Setting[_]](
  scalaVersion := SCALA_2_12,
  crossScalaVersions := targetScalaVersions,
  crossPaths := true,
  publishMavenStyle := true,
  scalacOptions ++= Seq("-feature", "-deprecation"), // ,"-Ytyper-debug"),
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
  testFrameworks += airSpecFramework,
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %%% "scala-collection-compat" % "2.1.1"
  )
)

// We need to define this globally as a workaround for https://github.com/sbt/sbt/pull/3760
publishTo in ThisBuild := sonatypePublishTo.value

val jsBuildSettings = Seq[Setting[_]](
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
    .aggregate(scaladoc)
    .aggregate((jvmProjects ++ jvmProjects2_12 ++ jsProjects): _*)

lazy val scaladoc =
  project
    .enablePlugins(ScalaUnidocPlugin)
    .in(file("airframe-scaladoc"))
    .settings(
      buildSettings,
      crossScalaVersions := targetScalaVersions,
      name := "airframe-scaladoc",
      // Need to exclude JS project explicitly to avoid '<type> is already defined' errors
      unidocProjectFilter in (ScalaUnidoc, unidoc) :=
        inAnyProject --
          inProjects(jvmProjects2_12: _*) --
          inProjects(airframeMacrosJS) --
          inProjects(jsProjects: _*) --
          inProjects(airspecProjects: _*),
      // compile projects first
      Defaults.packageTaskSettings(packageDoc in Compile, (unidoc in Compile).map(_.flatMap(Path.allSubpaths)))
    )
    .aggregate(jvmProjects: _*)

// JVM projects for scala-community build. This should have no tricky setup and should support Scala 2.12.
lazy val communityBuildProjects: Seq[ProjectReference] = Seq(
  airframeJVM,
  surfaceJVM,
  logJVM,
  airframeScalaTestJVM,
  canvas,
  config,
  control,
  jmx,
  launcher,
  metricsJVM,
  codecJVM,
  tablet,
  msgpackJVM,
  http,
  jsonJVM,
  airspecLogJVM,
  airspecCoreJVM,
  airspecJVM
)

// JVM projects that supports Scala 2.13
lazy val jvmProjects: Seq[ProjectReference] = communityBuildProjects ++ Seq[ProjectReference](
  jdbc,
  fluentd
)

// JVM projects excluded from Scala 2.13 build
lazy val jvmProjects2_12: Seq[ProjectReference] = Seq(
  finagle,
  httpRecorder,
  sql,
  benchmark,
  examples
)

// Scala.js build is only for Scala 2.12
lazy val jsProjects: Seq[ProjectReference] = Seq(
  airframeJS,
  surfaceJS,
  logJS,
  airframeScalaTestJS,
  metricsJS,
  codecJS,
  msgpackJS,
  jsonJS,
  airspecLogJS,
  airspecCoreJS,
  airspecJS
)

lazy val airspecProjects: Seq[ProjectReference] = Seq(
  airspecJVM,
  airspecJS,
  airspecCoreJVM,
  airspecCoreJS,
  airspecLogJVM,
  airspecLogJS
)

// For community-build
lazy val communityBuild =
  project
    .settings(
      noPublish,
      crossScalaVersions := targetScalaVersions
    )
    .aggregate(communityBuildProjects: _*)

// For Scala 2.12
lazy val projectJVM =
  project
    .settings(
      noPublish,
      crossScalaVersions := targetScalaVersions
    )
    .aggregate(jvmProjects ++ jvmProjects2_12: _*)

// For Scala 2.13 (excluding projects supporting only upto Scala 2.12)
lazy val projectJVM2_13 =
  project
    .settings(
      noPublish,
      crossScalaVersions := targetScalaVersions
    )
    // Generates unidoc
    .aggregate(scaladoc)
    .aggregate(jvmProjects: _*)

// For projects only upto Scala 2.12
lazy val projectJVM2_12 =
  project
    .settings(
      noPublish,
      crossScalaVersions := untilScala2_12
    )
    .aggregate(jvmProjects2_12: _*)

lazy val projectJS =
  project
    .settings(
      noPublish,
      crossScalaVersions := Seq(SCALA_2_12)
    )
    .aggregate(jsProjects: _*)

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
      micrositeDescription := "Lightweight Building Blocks for Scala",
      micrositeAuthor := "Taro L. Saito",
      micrositeOrganizationHomepage := "https://github.com/wvlet",
      micrositeHighlightTheme := "ocean",
      micrositeGithubOwner := "wvlet",
      micrositeGithubRepo := "airframe",
      micrositeUrl := "https://wvlet.org",
      micrositeBaseUrl := "airframe",
      micrositeAnalyticsToken := "UA-98364158-1",
      micrositeDocumentationUrl := "docs",
      micrositeGitterChannel := true,
      micrositeGitterChannelUrl := "wvlet/airframe",
      //micrositePushSiteWith := GitHub4s,
      //micrositeGithubToken := sys.env.get("GITHUB_REPO_TOKEN"),
      micrositePalette ++= Map(
        "brand-primary"   -> "#2582AA",
        "brand-secondary" -> "#143F56",
        "brand-tertiary"  -> "#042F46",
        "gray-dark"       -> "#453E46",
        "gray"            -> "#534F54"
      ),
      resourceGenerators in Tut += Def.task {
        // Copy source docs since Tut accepts only a single source directory
        val sourceDir = (sourceDirectory in Compile).value / "tut"
        IO.copyDirectory(sourceDir, tutSourceDirectory.value)

        // Generate airframe-xxx.md files from airframe-xxx/README.md
        generateModuleDoc(tutSourceDirectory.value, streams.value.log)
      }.taskValue,
      tutSourceDirectory := (managedResourceDirectories in Tut).value.head,
      watchSources += new sbt.internal.io.Source(
        sourceDirectory.value,
        new FileFilter {
          def accept(f: File) = !f.isDirectory
        },
        NothingFilter
      )
    )
    .enablePlugins(MicrositesPlugin)

def generateModuleDoc(targetDir: File, logger: Logger): Seq[File] = {
  val baseDir = file(".")
  val readmeFiles =
    baseDir
      .listFiles()
      .filter(x => x.isDirectory && (x.name.startsWith("airframe") || x.name.startsWith("airspec")))
      .map(x => x / "README.md")
      .filter(_.exists())

  logger.info("Generating module doc files")
  readmeFiles.flatMap { f =>
    f.relativeTo(baseDir).map { x =>
      val moduleName     = x.getParent
      val targetFileName = targetDir / "docs" / s"${moduleName}.md"
      logger.info(s"${targetFileName.relativeTo(baseDir).getOrElse(targetFileName.getName)}")

      val originalReadme = IO.read(f)
      val content =
        s"""---
           |layout: docs
           |title: ${moduleName}
           |---
           |${originalReadme}""".stripMargin

      IO.write(targetFileName, content)
      targetFileName
    }
  }
}

def parallelCollection(scalaVersion: String) = {
  if (scalaVersion.startsWith("2.13.")) {
    Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0")
  } else {
    Seq.empty
  }
}

lazy val airframe =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
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
      // Workaround for https://github.com/scala/scala/pull/7624 in Scala 2.13, and also
      // testing shtudown hooks requires consistent application lifecycle between sbt and JVM https://github.com/sbt/sbt/issues/4794
      fork in Test := scalaBinaryVersion.value == "2.13",
      // include the macro classes and resources in the main jar
      mappings in (Compile, packageBin) ++= mappings.in(airframeMacrosJVM, Compile, packageBin).value,
      // include the macro sources in the main source jar
      mappings in (Compile, packageSrc) ++= mappings.in(airframeMacrosJVM, Compile, packageSrc).value
    )
    .jsSettings(
      jsBuildSettings,
      // Copy macro classes into the main jar
      mappings in (Compile, packageBin) ++= mappings
        .in(airframeMacrosJS, Compile, packageBin).value.filter(x => x._2 != "JS_DEPENDENCIES"),
      // include the macro sources in the main source jar
      mappings in (Compile, packageSrc) ++= mappings.in(airframeMacrosJS, Compile, packageSrc).value
    )
    .dependsOn(surface, airframeMacrosRef, airframeScalaTest % "test")

lazy val airframeJVM = airframe.jvm
lazy val airframeJS  = airframe.js

// Airframe DI needs to call macro methods, so we needed to split the project into DI and DI macros.
lazy val airframeMacros =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-di-macros"))
    .settings(buildSettings)
    .settings(
      buildSettings,
      name := "airframe-di-macros",
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

// To use airframe in other airframe modules, we need to reference airframeMacros project using the internal scope
lazy val airframeMacrosJVMRef = airframeMacrosJVM % "compile-internal,test-internal"
lazy val airframeMacrosRef    = airframeMacros    % "compile-internal,test-internal"

val surfaceDependencies = { scalaVersion: String =>
  Seq(
    "org.scala-lang" % "scala-reflect"  % scalaVersion,
    "org.scala-lang" % "scala-compiler" % scalaVersion % "provided"
  )
}
val surfaceJVMDependencies = Seq(
  // For ading PreDestroy, PostConstruct annotations to Java9
  "javax.annotation" % "javax.annotation-api" % "1.3.1"
)

lazy val surface =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe-surface"))
    .settings(buildSettings)
    .settings(
      name := "airframe-surface",
      description := "A library for extracting object structure surface",
      libraryDependencies ++= surfaceDependencies(scalaVersion.value)
    )
    .jvmSettings(
      libraryDependencies ++= surfaceJVMDependencies
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log, airspec % "test")

lazy val surfaceJVM = surface.jvm
lazy val surfaceJS  = surface.js

lazy val canvas =
  project
    .in(file("airframe-canvas"))
    .settings(buildSettings)
    .settings(
      name := "airframe-canvas",
      description := "Airframe off-heap memory library",
      libraryDependencies ++= Seq(
        "org.scalacheck" %%% "scalacheck" % SCALACHECK_VERSION % "test"
      )
    )
    .dependsOn(logJVM, control % "test", airframeScalaTestJVM % "test")

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
    .dependsOn(airframeJVM, airframeMacrosJVMRef, tablet, airspecJVM % "test")

lazy val control =
  project
    .in(file("airframe-control"))
    .settings(buildSettings)
    .settings(
      name := "airframe-control",
      description := "A library for controlling program flows and retrying",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % SCALA_PARSER_COMBINATOR_VERSION
      )
    )
    .dependsOn(logJVM, jmx, airspecJVM % "test")

lazy val jmx =
  project
    .in(file("airframe-jmx"))
    .settings(buildSettings)
    .settings(
      name := "airframe-jmx",
      description := "A library for exposing Scala object data through JMX"
    )
    .dependsOn(surfaceJVM, airspecJVM % "test")

lazy val launcher =
  project
    .in(file("airframe-launcher"))
    .settings(buildSettings)
    .settings(
      name := "airframe-launcher",
      description := "Command-line program launcher",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % SCALA_PARSER_COMBINATOR_VERSION
      )
    )
    .dependsOn(surfaceJVM, control, codecJVM, airspecJVM % "test")

val logDependencies = { scalaVersion: String =>
  Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion % "provided"
  )
}
val logJVMDependencies = Seq(
  "ch.qos.logback" % "logback-core" % "1.2.3"
)

// airframe-log should have minimum dependencies
lazy val log: sbtcrossproject.CrossProject =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe-log"))
    .settings(buildSettings)
    .settings(
      name := "airframe-log",
      description := "Fancy logger for Scala",
      libraryDependencies ++= logDependencies(scalaVersion.value)
    )
    .jvmSettings(
      libraryDependencies ++= logJVMDependencies,
      //classLoaderLayeringStrategy in Test := ClassLoaderLayeringStrategy.AllLibraryJars
    )
    .jsSettings(
      jsBuildSettings,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-java-logging" % JS_JAVA_LOGGING_VERSION
      )
    )
    .dependsOn(airspec % "test")

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
    .dependsOn(log, surface, airspec % "test")

lazy val metricsJVM = metrics.jvm
lazy val metricsJS  = metrics.js

lazy val airframeScalaTest =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe-scalatest"))
    .settings(buildSettings)
    .settings(
      name := "airframe-scalatest",
      description := "A handy base trait for writing test using ScalaTest",
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % SCALATEST_VERSION
      )
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log)

lazy val airframeScalaTestJVM = airframeScalaTest.jvm
lazy val airframeScalaTestJS  = airframeScalaTest.js

lazy val msgpack =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-msgpack"))
    .settings(buildSettings)
    .settings(
      name := "airframe-msgpack",
      description := "Pure-Scala MessagePack library",
      libraryDependencies ++= Seq(
        "org.scalacheck" %%% "scalacheck" % SCALACHECK_VERSION % "test"
      )
    )
    .jvmSettings(
      libraryDependencies += "org.msgpack" % "msgpack-core" % MSGPACK_VERSION
    )
    .jsSettings(
      jsBuildSettings,
      libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.5"
    )
    .dependsOn(log, airframeScalaTest % "test")

lazy val msgpackJVM = msgpack.jvm
lazy val msgpackJS  = msgpack.js

lazy val codec =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-codec"))
    .settings(buildSettings)
    .settings(
      name := "airframe-codec",
      description := "Airframe MessagePack-based codec",
      libraryDependencies ++= Seq(
        "org.scalacheck" %%% "scalacheck" % SCALACHECK_VERSION % "test"
      )
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        // For JDBC testing
        "org.xerial" % "sqlite-jdbc" % SQLITE_JDBC_VERSION % "test"
      )
    )
    .jsSettings(
      jsBuildSettings
    )
    .dependsOn(log, surface, msgpack, metrics, json, airframeScalaTest % "test")

lazy val codecJVM = codec.jvm
lazy val codecJS  = codec.js

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
        // For ColumnType parser
        "org.scala-lang.modules" %% "scala-parser-combinators" % SCALA_PARSER_COMBINATOR_VERSION,
        "org.scalacheck"         %% "scalacheck"               % SCALACHECK_VERSION % "test",
        "org.msgpack"            % "msgpack-core"              % "0.8.14",
        // For JDBC testing
        "org.xerial" % "sqlite-jdbc" % SQLITE_JDBC_VERSION % "test"
      )
    )
    .dependsOn(codecJVM, logJVM, surfaceJVM, airframeScalaTestJVM % "test")

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
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(airframeJVM, airframeMacrosJVMRef, airframeScalaTestJVM % "test")

lazy val http =
  project
    .in(file("airframe-http"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http",
      description := "JAX-RS based REST API Framework",
      libraryDependencies ++= Seq(
        )
    )
    .dependsOn(airframeJVM, airframeMacrosJVMRef, control, surfaceJVM, jsonJVM, codecJVM, airframeScalaTestJVM % "test")

val FINAGLE_VERSION = "19.6.0"
lazy val finagle =
  project
    .in(file("airframe-http-finagle"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http-finagle",
      description := "REST API binding for Finagle",
      // A workaround for the dealy of slf4j-jdk14 log flush after sbt class loader is detached
      fork in Test := true,
      // Finagle doesn't support Scala 2.13 yet
      crossScalaVersions := untilScala2_12,
      libraryDependencies ++= Seq(
        "com.twitter" %% "finagle-http"        % FINAGLE_VERSION,
        "com.twitter" %% "finagle-netty4-http" % FINAGLE_VERSION,
        "com.twitter" %% "finagle-netty4"      % FINAGLE_VERSION,
        "com.twitter" %% "finagle-core"        % FINAGLE_VERSION,
        // Redirecting slf4j log in Finagle to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(http, airframeMacrosJVMRef, airframeScalaTestJVM % "test")

lazy val httpRecorder =
  project
    .in(file("airframe-http-recorder"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http-recorder",
      description := "Http Response Recorder",
      // Finagle doesn't support Scala 2.13 yet
      crossScalaVersions := untilScala2_12,
      libraryDependencies ++= Seq(
        "com.twitter" %% "finatra-http"        % FINAGLE_VERSION,
        "com.twitter" %% "finagle-netty4-http" % FINAGLE_VERSION,
        "com.twitter" %% "finagle-netty4"      % FINAGLE_VERSION,
        "com.twitter" %% "finagle-core"        % FINAGLE_VERSION,
        // Redirecting slf4j log in Finagle to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(codecJVM,
               metricsJVM,
               control,
               finagle,
               jdbc,
               tablet,
               airframeMacrosJVMRef,
               airframeScalaTestJVM % "test")

lazy val json =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-json"))
    .settings(buildSettings)
    .settings(
      name := "airframe-json",
      description := "JSON parser"
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log, airframeScalaTest % "test")

lazy val jsonJVM = json.jvm
lazy val jsonJS  = json.js

val JMH_VERSION = "1.21"

import xerial.sbt.pack.PackPlugin._

lazy val benchmark =
  project
    .in(file("airframe-benchmark"))
    // Necessary for generating /META-INF/BenchmarkList
    .enablePlugins(JmhPlugin)
    .enablePlugins(PackPlugin)
    .settings(buildSettings)
    .settings(
      name := "airframe-benchmark",
      packMain := Map("airframe-benchmark" -> "wvlet.airframe.benchmark.BenchmarkMain"),
      // Generate JMH benchmark cord before packaging and testing
      pack := pack.dependsOn(compile in Test).value,
      sourceDirectory in Jmh := (sourceDirectory in Compile).value,
      compile in Test := ((compile in Test).dependsOn(compile in Jmh)).value,
      // Need to fork JVM so that sbt can set the classpass properly for running JMH
      fork in run := true,
      crossScalaVersions := untilScala2_12,
      libraryDependencies ++= Seq(
        "org.msgpack"     % "msgpack-core"             % MSGPACK_VERSION,
        "org.openjdk.jmh" % "jmh-core"                 % JMH_VERSION,
        "org.openjdk.jmh" % "jmh-generator-bytecode"   % JMH_VERSION,
        "org.openjdk.jmh" % "jmh-generator-reflection" % JMH_VERSION,
        // Used only for json benchmark
        "org.json4s" %% "json4s-jackson" % "3.6.7",
        "io.circe"   %% "circe-parser"   % "0.11.1"
      ),
      publishPackArchiveTgz
    )
    .dependsOn(msgpackJVM, jsonJVM, metricsJVM, launcher, airframeScalaTestJVM % "test")

lazy val fluentd =
  project
    .in(file("airframe-fluentd"))
    .settings(buildSettings)
    .settings(
      name := "airframe-fluentd",
      description := "Fluentd logger",
      libraryDependencies ++= Seq(
        "org.komamitsu" % "fluency-core"         % "2.3.2",
        "org.komamitsu" % "fluency-fluentd"      % "2.3.2",
        "org.komamitsu" % "fluency-treasuredata" % "2.3.2" % "provided",
        // Redirecting slf4j log from Fluency to aiframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(codecJVM, airframeJVM, airframeMacrosJVMRef, airframeScalaTestJVM % "test")

lazy val sql =
  project
    .enablePlugins(Antlr4Plugin)
    .in(file("airframe-sql"))
    .settings(buildSettings)
    .settings(
      name := "airframe-sql",
      description := "SQL parser & analyzer",
      antlr4Version in Antlr4 := "4.7.2",
      antlr4PackageName in Antlr4 := Some("wvlet.airframe.sql.parser"),
      antlr4GenListener in Antlr4 := true,
      antlr4GenVisitor in Antlr4 := true,
      libraryDependencies ++= Seq(
        // For parsing DataType strings
        "org.scala-lang.modules" %% "scala-parser-combinators" % SCALA_PARSER_COMBINATOR_VERSION,
        // Include Spark just as a reference implementation
        "org.apache.spark" %% "spark-sql" % "2.4.0" % "test"
      )
    )
    .dependsOn(msgpackJVM, surfaceJVM, config, launcher, airframeScalaTestJVM % "test")

lazy val examples =
  project
    .in(file("examples"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      name := "airframe-examples",
      description := "Airframe examples",
      crossScalaVersions := untilScala2_12,
      libraryDependencies ++= Seq(
        )
    )
    .dependsOn(codecJVM,
               airframeJVM,
               config,
               airframeMacrosJVM,
               launcher,
               jmx,
               jdbc,
               tablet,
               finagle,
               airframeScalaTestJVM % "test")

/**
  * AirSpec build definitions.
  *
  * To make airspec a standalone library without any cyclic project references, airspec shares the same source code with airframe-log, di, surface, etc.
  *
  * Since airframe-log, di, and surfaces uses Scala macros whose def-macros cannot be called within the same project,
  * we need to split the source code into three projects:
  *
  *  - airspec-log (dependsOn airframe-log's source)
  *  - airspec-core (di-macros, surface)
  *  - airspec (di, metrics)
  *
  */
val airspecLogDependencies  = Seq("airframe-log")
val airspecCoreDependencies = Seq("airframe-di-macros", "airframe-surface")
val airspecDependencies     = Seq("airframe", "airframe-metrics")

// Setting keys for AirSpec
val airspecDependsOn = settingKey[Seq[String]]("Dependent module names of airspec projects")

val airspecBuildSettings = Seq[Setting[_]](
  unmanagedSourceDirectories in Compile ++= {
    val baseDir = baseDirectory.value.getParentFile.getParentFile.getAbsolutePath
    val sourceDirs = for (m <- airspecDependsOn.value) yield {
      Seq(
        file(s"${baseDir}/${m}/src/main/scala"),
        file(s"${baseDir}/${m}/shared/src/main/scala")
      )
    }
    sourceDirs.flatten
  }
)

val airspecJVMBuildSettings = Seq[Setting[_]](
  unmanagedSourceDirectories in Compile ++= {
    val baseDir = baseDirectory.value.getParentFile.getParentFile.getAbsolutePath
    val sv      = scalaBinaryVersion.value
    val sourceDirs = for (m <- airspecDependsOn.value) yield {
      Seq(
        file(s"${baseDir}/${m}/.jvm/src/main/scala"),
        file(s"${baseDir}/${m}/.jvm/src/main/scala-${sv}"),
        file(s"${baseDir}/${m}/jvm/src/main/scala"),
        file(s"${baseDir}/${m}/jvm/src/main/scala-${sv}")
      )
    }
    sourceDirs.flatten
  }
)

val airspecJSBuildSettings = Seq[Setting[_]](
  unmanagedSourceDirectories in Compile ++= {
    val baseDir = baseDirectory.value.getParentFile.getParentFile.getAbsolutePath
    val sv      = scalaBinaryVersion.value
    val sourceDirs = for (m <- airspecDependsOn.value) yield {
      Seq(
        file(s"${baseDir}/${m}/.js/src/main/scala"),
        file(s"${baseDir}/${m}/.js/src/main/scala-${sv}"),
        file(s"${baseDir}/${m}/js/src/main/scala"),
        file(s"${baseDir}/${m}/js/src/main/scala-${sv}")
      )
    }
    sourceDirs.flatten
  }
)

lazy val airspecLog =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("airspec-log"))
    .settings(buildSettings)
    .settings(
      airspecDependsOn := airspecLogDependencies,
      airspecBuildSettings,
      name := "airspec-log",
      description := "airframe-log for AirSpec",
      libraryDependencies ++= logDependencies(scalaVersion.value)
    )
    .jvmSettings(
      airspecJVMBuildSettings,
      libraryDependencies ++= logJVMDependencies
    )
    .jsSettings(
      airspecJSBuildSettings,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-java-logging" % JS_JAVA_LOGGING_VERSION
      )
    )

lazy val airspecLogJVM = airspecLog.jvm
lazy val airspecLogJS  = airspecLog.js

lazy val airspecCore =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("airspec-core"))
    .settings(buildSettings)
    .settings(
      airspecDependsOn := airspecCoreDependencies,
      airspecBuildSettings,
      name := "airspec-core",
      description := "A core module of AirSpec with Surface and DI macros",
      libraryDependencies ++= surfaceDependencies(scalaVersion.value)
    )
    .jvmSettings(
      airspecJVMBuildSettings,
      libraryDependencies ++= surfaceJVMDependencies
    )
    .jsSettings(
      airspecJSBuildSettings,
    )
    .dependsOn(airspecLog)

lazy val airspecCoreJVM = airspecCore.jvm
lazy val airspecCoreJS  = airspecCore.js

lazy val airspec =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("airspec"))
    .settings(buildSettings)
    .settings(
      airspecDependsOn := airspecDependencies,
      airspecBuildSettings,
      name := "airspec",
      description := "AirSpec: A Functional Testing Framework for Scala"
    )
    .jvmSettings(
      airspecJVMBuildSettings,
      libraryDependencies ++= Seq(
        "org.scala-sbt" % "test-interface" % "1.0"
      )
    )
    .jsSettings(
      airspecJSBuildSettings,
      libraryDependencies ++= Seq(
        "org.scala-js"       %% "scalajs-test-interface"  % SCALA_JS_VERSION,
        "org.portable-scala" %%% "portable-scala-reflect" % "0.1.0"
      )
    )
    .dependsOn(airspecCore)

lazy val airspecJVM = airspec.jvm
lazy val airspecJS  = airspec.js
