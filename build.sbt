import sbtcrossproject.{crossProject, CrossType}

val SCALA_2_12 = "2.12.8"
val SCALA_2_13 = "2.13.0-M5"
val SCALA_2_11 = "2.11.12"

val untilScala2_12      = SCALA_2_12 :: SCALA_2_11 :: Nil
val targetScalaVersions = SCALA_2_13 :: untilScala2_12

val SCALATEST_VERSION               = "3.0.6-SNAP4"
val SCALACHECK_VERSION              = "1.14.0"
val SCALA_PARSER_COMBINATOR_VERSION = "1.1.1"
val SQLITE_JDBC_VERSION             = "3.21.0.1"
val SLF4J_VERSION                   = "1.7.25"

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

inThisBuild(if (isTravisBuild) travisSettings else List.empty)

val buildSettings = Seq[Setting[_]](
  scalaVersion := SCALA_2_12,
  crossScalaVersions := targetScalaVersions,
  crossPaths := true,
  publishMavenStyle := true,
  logBuffered in Test := false,
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
  // Use sonatype resolvers
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases")
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
          inProjects(jsProjects: _*),
      // compile projects first
      Defaults.packageTaskSettings(packageDoc in Compile, (unidoc in Compile).map(_.flatMap(Path.allSubpaths)))
    )
    .aggregate(jvmProjects: _*)

// JVM projects for scala-community build. This should have no tricky setup and should support Scala 2.12.
lazy val communityBuildProjects: Seq[ProjectReference] = Seq(
  airframeJVM,
  surfaceJVM,
  logJVM,
  airframeSpecJVM,
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
  jsonJVM
)

// JVM projects that supports Scala 2.13
lazy val jvmProjects: Seq[ProjectReference] = communityBuildProjects ++ Seq[ProjectReference](
  jdbc,
  fluentd
)

// JVM projects that cannot be build in Scala 2.13
lazy val jvmProjects2_12: Seq[ProjectReference] = Seq(
  finagle,
  httpRecorder,
  jsonBenchmark
)

// Scala.js builds is only for Scala 2.12
lazy val jsProjects: Seq[ProjectReference] = Seq(
  airframeJS,
  surfaceJS,
  logJS,
  airframeSpecJS,
  metricsJS,
  codecJS,
  msgpackJS,
  jsonJS
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
    .dependsOn(surface, airframeMacrosRef, airframeSpec % "test")

lazy val airframeJVM = airframe.jvm
lazy val airframeJS  = airframe.js

// Airframe depends on Airframe Macros, so we needed to split the project
lazy val airframeMacros =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
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

// To use airframe in other airframe modules, we need to reference airframeMacros project using the internal scope
lazy val airframeMacrosJVMRef = airframeMacrosJVM % "compile-internal,test-internal"
lazy val airframeMacrosRef    = airframeMacros    % "compile-internal,test-internal"

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
    .jvmSettings(
      libraryDependencies ++= Seq(
        // For ading PreDestroy, PostConstruct annotations to Java9
        "javax.annotation" % "javax.annotation-api" % "1.3.1"
      )
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log, airframeSpec % "test")

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
    .dependsOn(logJVM, control % "test", airframeSpecJVM % "test")

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
    .dependsOn(airframeJVM, airframeMacrosJVMRef, tablet, airframeSpecJVM % "test")

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
    .dependsOn(logJVM, airframeSpecJVM % "test")

lazy val jmx =
  project
    .in(file("airframe-jmx"))
    .settings(buildSettings)
    .settings(
      name := "airframe-jmx",
      description := "A library for exposing Scala object data through JMX"
    )
    .dependsOn(surfaceJVM, airframeSpecJVM % "test")

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
    .dependsOn(surfaceJVM, control, codecJVM, airframeSpecJVM % "test")

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
      )
    )
    .jvmSettings(
      libraryDependencies ++= Seq("ch.qos.logback" % "logback-core" % "1.2.3")
    )
    .jsSettings(
      jsBuildSettings,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-java-logging" % "0.1.4"
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
      )
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log)

lazy val airframeSpecJVM = airframeSpec.jvm
lazy val airframeSpecJS  = airframeSpec.js

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
      libraryDependencies += "org.msgpack" % "msgpack-core" % "0.8.16"
    )
    .jsSettings(
      jsBuildSettings,
      libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.5"
    )
    .dependsOn(log, airframeSpec % "test")

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
    .dependsOn(log, surface, msgpack, metrics, json, airframeSpec % "test")

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
    .dependsOn(codecJVM, logJVM, surfaceJVM, airframeSpecJVM % "test")

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
    .dependsOn(airframeJVM, airframeMacrosJVMRef, airframeSpecJVM % "test")

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
    .dependsOn(airframeJVM, airframeMacrosJVMRef, surfaceJVM, jsonJVM, codecJVM, airframeSpecJVM % "test")

val FINAGLE_VERSION = "19.1.0"
lazy val finagle =
  project
    .in(file("airframe-http-finagle"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http-finagle",
      description := "REST API binding for Finagle",
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
    .dependsOn(http, airframeMacrosJVMRef, airframeSpecJVM % "test")

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
    .dependsOn(codecJVM, metricsJVM, control, finagle, jdbc, tablet, airframeMacrosJVMRef, airframeSpecJVM % "test")

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
    .dependsOn(log, airframeSpec % "test")

lazy val jsonJVM = json.jvm
lazy val jsonJS  = json.js

lazy val jsonBenchmark =
  project
    .in(file("airframe-json-benchmark"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      name := "airframe-json-benchmark",
      description := "JSON parser benchmark",
      crossScalaVersions := untilScala2_12,
      libraryDependencies ++= Seq(
        // Used only for benchmark purpose
        "org.json4s" %% "json4s-native"  % "3.5.4",
        "org.json4s" %% "json4s-jackson" % "3.5.4",
        //"org.spire-math" %% "jawn-ast"       % "0.13.0",
        "io.circe"    %% "circe-parser" % "0.9.3",
        "com.lihaoyi" %% "ujson"        % "0.6.6"
      )
    )
    .dependsOn(jsonJVM, airframeSpecJVM % "test")

lazy val fluentd =
  project
    .in(file("airframe-fluentd"))
    .settings(buildSettings)
    .settings(
      name := "airframe-fluentd",
      description := "Fluentd logger",
      libraryDependencies ++= Seq(
        "org.komamitsu" % "fluency" % "1.8.1",
        // Redirecting slf4j log from Fluency to aiframe-log
        "org.slf4j"  % "slf4j-jdk14"         % SLF4J_VERSION,
        "org.xerial" %% "fluentd-standalone" % "1.2.6.1" % "test"
      )
    )
    .dependsOn(codecJVM, airframeJVM, airframeMacrosJVMRef, airframeSpecJVM % "test")
