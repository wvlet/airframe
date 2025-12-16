import scalajsbundler.JSDOMNodeJSEnv
import xerial.sbt.pack.PackPlugin.{projectSettings, publishPackArchiveTgz}

val SCALA_2_12          = "2.12.21"
val SCALA_2_13          = "2.13.18"
val SCALA_3             = sys.env.getOrElse("SCALA_VERSION", "3.3.7")
val uptoScala2          = SCALA_2_13 :: SCALA_2_12 :: Nil
val targetScalaVersions = SCALA_3 :: uptoScala2

// Add this for using snapshot versions
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

val AIRSPEC_VERSION                 = sys.env.getOrElse("AIRSPEC_VERSION", "24.12.1")
val SCALACHECK_VERSION              = "1.19.0"
val MSGPACK_VERSION                 = "0.9.10"
val SCALA_PARSER_COMBINATOR_VERSION = "2.4.0"
val SQLITE_JDBC_VERSION             = "3.51.1.0"
val SLF4J_VERSION                   = "2.0.17"
val JS_JAVA_LOGGING_VERSION         = "1.0.0"
val JS_JAVA_TIME_VERSION            = "1.0.0"
val SCALAJS_DOM_VERSION             = "2.8.1"
val FINAGLE_VERSION                 = "24.2.0"
val FLUENCY_VERSION                 = "2.7.3"
val GRPC_VERSION                    = "1.77.0"
val JMH_VERSION                     = "1.37"
val JAVAX_ANNOTATION_API_VERSION    = "1.3.2"
val PARQUET_VERSION                 = "1.16.0"
val SNAKE_YAML_VERSION              = "2.5"

val AIRFRAME_BINARY_COMPAT_VERSION = "23.6.0"

// JVM options for Java 24+
val java24PlusJvmOptions = Seq("--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED")

// A short cut for publishing snapshots to Sonatype
addCommandAlias(
  "publishSnapshots",
  s"+ projectJVM/publish; + projectJS/publish; + projectNative/publish"
)

// [Development purpose] publish all artifacts to the local repo
addCommandAlias(
  "publishAllLocal",
  s"+ projectJVM/publishLocal; + projectJS/publishLocal; + projectNative/publishLocal"
)

// [Development purpose] publish all sbt-airframe related artifacts to local repo
addCommandAlias(
  "publishSbtDevLocal",
  s"++ 2.12; projectJVM/publishLocal; ++ 3; projectDotty/publishLocal; projectJS/publishLocal"
)

addCommandAlias(
  "publishJSSigned",
  s"+ projectJS/publishSigned"
)
addCommandAlias(
  "publishJSLocal",
  s"+ projectJS/publishLocal"
)
addCommandAlias(
  "publishNativeSigned",
  s"+ projectNative/publishSigned"
)

// Allow using Ctrl+C in sbt without exiting the prompt
// Global / cancelable := true

//ThisBuild / turbo := true

// Reload build.sbt on changes
Global / onChangedBuildSource := ReloadOnSourceChanges

// ideSkipProject is used only for IntelliJ IDEA
Global / excludeLintKeys ++= Set(ideSkipProject)

// Disable the pipelining available since sbt-1.4.0. It caused compilation failure
ThisBuild / usePipelining := false

// Use Scala 3 by default as scala-2 specific source code is relatively small now
ThisBuild / scalaVersion := SCALA_3

ThisBuild / organization := "org.wvlet.airframe"

// Use dynamic snapshot version strings for non tagged versions
ThisBuild / dynverSonatypeSnapshots := true
// Use coursier friendly version separator
ThisBuild / dynverSeparator := "-"

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
  // Exclude compile-time only projects. This is a workaround for bloop,
  // which cannot resolve Optional dependencies nor compile-internal dependencies.
  pomPostProcess        := excludePomDependency(Seq("airspec_2.12", "airspec_2.13", "airspec_3")),
  crossScalaVersions    := targetScalaVersions,
  crossPaths            := true,
  publishMavenStyle     := true,
  mimaPreviousArtifacts := Set("org.wvlet.airframe" %%% s"${name.value}" % AIRFRAME_BINARY_COMPAT_VERSION),
  mimaFailOnNoPrevious  := false,
  mimaBinaryIssueFilters ++= {
    import com.typesafe.tools.mima.core.*
    Seq(
      ProblemFilters.exclude[MissingClassProblem]("wvlet.airframe.http.internal.*")
    )
  },
  javacOptions ++= Seq("-source", "11", "-target", "11"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation"
    // Use this flag for debugging Macros
    // "-Xcheck-macros",
  ) ++ {
    if (scalaVersion.value.startsWith("3.")) {
      Seq.empty
    } else {
      Seq(
        // Necessary for tracking source code range in airframe-rx demo
        "-Yrangepos",
        // For using the new import * syntax even in Scala 2.x
        "-Xsource:3"
      )
    }
  },
  testFrameworks += new TestFramework("wvlet.airspec.Framework"),
  libraryDependencies ++= Seq(
    "org.wvlet.airframe" %%% "airspec"    % AIRSPEC_VERSION    % Test,
    "org.scalacheck"     %%% "scalacheck" % SCALACHECK_VERSION % Test
  ) ++ {
    if (scalaVersion.value.startsWith("3."))
      Seq.empty
    else
      Seq("org.scala-lang.modules" %%% "scala-collection-compat" % "2.14.0")
  }
)

val scala2Only = Seq[Setting[?]](
  scalaVersion       := SCALA_2_13,
  crossScalaVersions := uptoScala2
)

val scala3Only = Seq[Setting[?]](
  scalaVersion       := SCALA_3,
  crossScalaVersions := List(SCALA_3)
)

// Do not run tests concurrently to avoid JMX registration failures
val runTestSequentially = Seq[Setting[?]](Test / parallelExecution := false)

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

val jsBuildSettings = Seq[Setting[?]](
  // #2117 For using java.util.UUID.randomUUID() in Scala.js
  libraryDependencies ++= Seq(
    ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0" % Test).cross(CrossVersion.for3Use2_13),
    // TODO It should be included in AirSpec
    "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1" % Test
  )
)

val nativeBuildSettings = Seq[Setting[?]](
  scalaVersion       := SCALA_3,
  crossScalaVersions := List(SCALA_3)
//  nativeConfig ~= {
//    _.withSourceLevelDebuggingConfig(_.enableAll) // enable generation of debug informations
//      .withOptimize(false)                        // disable Scala Native optimizer
//      .withMode(scalanative.build.Mode.debug)     // compile using LLVM without optimizations
//  }
)

val noPublish = Seq(
  publishArtifact := false,
  publish         := {},
  publishLocal    := {},
  publish / skip  := true,
  // This must be Nil to use crossScalaVersions of individual modules in `+ projectJVM/xxxx` tasks
  crossScalaVersions := Nil,
  // Explicitly skip the doc task because protobuf related Java files causes no type found error
  Compile / doc / sources                := Seq.empty,
  Compile / packageDoc / publishArtifact := false,
  // Do not check binary compatibility for unpublished projects
  mimaPreviousArtifacts := Set.empty
)

lazy val root =
  project
    .in(file("."))
    .settings(name := "airframe-root")
    .settings(buildSettings)
    .settings(noPublish)
    .aggregate((jvmProjects ++ jsProjects ++ itProjects): _*)

// JVM projects for scala-community build. This should have no tricky setup and should support Scala 2.12 and Scala 3
lazy val communityBuildProjects: Seq[ProjectReference] = Seq(
  canvas,
  config,
  control.jvm,
  codec.jvm,
  diMacros.jvm,
  di.jvm,
  fluentd,
  grpc,
  http.jvm,
  httpCodeGen,
  httpRecorder,
  jdbc,
  jmx,
  json.jvm,
  log.jvm,
  launcher,
  metrics.jvm,
  msgpack.jvm,
  netty,
  okhttp,
  parquet,
  rx.jvm,
  rxHtml.jvm,
  surface.jvm,
  ulid.jvm,
  examples
)

// Other JVM projects supporting Scala 2.12 - Scala 2.13
lazy val jvmProjects: Seq[ProjectReference] = communityBuildProjects ++ Seq[ProjectReference](
  finagle,
  benchmark,
  sql
)

// Scala.js build (Scala 2.12, 2.13, and 3.x)
lazy val jsProjects: Seq[ProjectReference] = Seq(
  log.js,
  surface.js,
  diMacros.js,
  di.js,
  metrics.js,
  control.js,
  ulid.js,
  json.js,
  msgpack.js,
  codec.js,
  http.js,
  rx.js,
  rxHtml.js,
  widgetJS
)

lazy val nativeProjects: Seq[ProjectReference] = Seq(
  log.native,
  surface.native,
  diMacros.native,
  di.native,
  metrics.native,
  json.native,
  msgpack.native,
  ulid.native,
  rx.native,
  control.native,
  codec.native,
  http.native
)

// Integration test projects
lazy val itProjects: Seq[ProjectReference] = Seq(
  integrationTestApi.jvm,
  integrationTestApi.js,
  integrationTest,
  integrationTestJs
)

// For community-build
lazy val communityBuild =
  project
    .settings(noPublish)
    .settings(
      // Skip importing aggregated projects in IntelliJ IDEA
      ideSkipProject := true
    )
    .aggregate(communityBuildProjects: _*)

// For Scala 2.12
lazy val projectJVM =
  project
    .settings(noPublish)
    .settings(
      // Skip importing aggregated projects in IntelliJ IDEA
      ideSkipProject := true
    )
    .aggregate(jvmProjects: _*)

lazy val projectJS =
  project
    .settings(noPublish)
    .settings(
      // Skip importing aggregated projects in IntelliJ IDEA
      ideSkipProject := true
    )
    .aggregate(jsProjects: _*)

lazy val projectNative =
  project
    .settings(noPublish)
    .settings(
      // Skip importing aggregated projects in IntelliJ IDEA
      ideSkipProject := true
    )
    .aggregate(nativeProjects: _*)

lazy val projectIt =
  project
    .settings(noPublish)
    .settings(
      // Skip importing aggregated projects in IntelliJ IDEA
      ideSkipProject := true
    )
    .aggregate(itProjects: _*)

// A scoped project only for Dotty (Scala 3).
// This is a workaround as projectJVM/test shows compile errors for non Scala 3 ready projects
lazy val projectDotty =
  project
    .settings(noPublish)
    .settings(
      // Skip importing aggregated projects in IntelliJ IDEA
      ideSkipProject := true
    )
    .aggregate(
      diMacros.jvm,
      di.jvm,
      log.jvm,
      surface.jvm,
      canvas,
      control.jvm,
      config,
      codec.jvm,
      fluentd,
      http.jvm,
      netty,
      httpCodeGen,
      httpRecorder,
      okhttp,
      // // Finagle isn't supporting Scala 3
      // httpFinagle,
      grpc,
      jdbc,
      jmx,
      launcher,
      metrics.jvm,
      msgpack.jvm,
      json.jvm,
      parquet,
      rx.jvm,
      rxHtml.jvm,
      sql,
      ulid.jvm,
      examples
    )

lazy val docs =
  project
    .in(file("airframe-docs"))
    .settings(
      name            := "airframe-docs",
      moduleName      := "airframe-docs",
      publishArtifact := false,
      publish         := {},
      publishLocal    := {},
      mdoc / watchTriggers += ((ThisBuild / baseDirectory).value / "docs").toGlob / ** / "*.md"
    )
    .enablePlugins(MdocPlugin, DocusaurusPlugin)

def parallelCollection(scalaVersion: String) = {
  if (scalaVersion.startsWith("2.13.")) {
    Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0")
  } else {
    Seq.empty
  }
}

// https://stackoverflow.com/questions/41670018/how-to-prevent-sbt-to-include-test-dependencies-into-the-pom
import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, *}
import scala.xml.transform.{RewriteRule, RuleTransformer}

def excludePomDependency(excludes: Seq[String]) = { node: XmlNode =>
  def isExcludeTarget(artifactId: String): Boolean =
    excludes.exists(artifactId.startsWith(_))

  def artifactId(e: Elem): Option[String] =
    e.child.find(_.label == "artifactId").map(_.text.trim())

  new RuleTransformer(new RewriteRule {
    override def transform(node: XmlNode): XmlNodeSeq =
      node match {
        case e: Elem
            if e.label == "dependency"
              && artifactId(e).exists(id => isExcludeTarget(id)) =>
          Comment(s"Excluded compile-time only dependency: ${artifactId(e).getOrElse("")}")
        case _ =>
          node
      }
  }).transform(node).head
}

lazy val base =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-core-base"))
    .settings(buildSettings)
    .settings(scala3Only)
    .settings(
      name        := "airframe-core-base",
      description := "Macro and base module for airframe-core"
    )
    .jsSettings(jsBuildSettings)
    .nativeSettings(nativeBuildSettings)

lazy val core =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-core"))
    .settings(buildSettings)
    .settings(scala3Only)
    .settings(
      name        := "airframe-core",
      description := "A new core module of Airframe for Scala 3"
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        // TODO Add pure-Scala/Java code for rotating log files
        "ch.qos.logback" % "logback-core" % "1.5.8"
      )
    )
    .jsSettings(jsBuildSettings)
    .nativeSettings(nativeBuildSettings)
    .dependsOn(base)

def airframeDIDependencies = Seq(
  "javax.annotation" % "javax.annotation-api" % JAVAX_ANNOTATION_API_VERSION
)

lazy val di =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-di"))
    .settings(buildSettings)
    .settings(
      name        := "airframe",
      description := "Dependency injection library tailored to Scala",
      // For PreDestroy, PostConstruct annotations
      libraryDependencies ++= airframeDIDependencies
    )
    .jvmSettings(
      // Workaround for https://github.com/scala/scala/pull/7624 in Scala 2.13, and also
      // testing shutdown hooks requires consistent application lifecycle between sbt and JVM https://github.com/sbt/sbt/issues/4794
      Test / fork := scalaBinaryVersion.value == "2.13"
    )
    .jsSettings(
      jsBuildSettings
    )
    .nativeSettings(
      nativeBuildSettings
    )
    .dependsOn(
      surface,
      diMacros
    )

def crossBuildSources(scalaBinaryVersion: String, baseDir: String, srcType: String = "main"): Seq[sbt.File] = {
  val scalaMajorVersion = scalaBinaryVersion.split("\\.").head
  for (suffix <- Seq("", s"-${scalaBinaryVersion}", s"-${scalaMajorVersion}").distinct)
    yield {
      file(s"${baseDir}/src/${srcType}/scala${suffix}")
    }
}

// Airframe DI needs to call macro methods, so we needed to split the project into DI and DI macros.
// This project sources and classes will be embedded to airframe.jar, so we don't publish airframe-di-macros
lazy val diMacros =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-di-macros"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-di-macros",
      description := "Macros for Airframe Di"
    )
    .jsSettings(jsBuildSettings)
    .nativeSettings(nativeBuildSettings)
    .dependsOn(log, surface)

// // To use airframe in other airframe modules, we need to reference airframeMacros project
// lazy val airframeMacrosJVMRef = airframeMacrosJVM % Optional
// lazy val airframeMacrosRef    = airframeMacros    % Optional
val surfaceDependencies = { scalaVersion: String =>
  scalaVersion match {
    case s if s.startsWith("3.") =>
      Seq.empty
    case _ =>
      Seq(
        ("org.scala-lang" % "scala-reflect"  % scalaVersion),
        ("org.scala-lang" % "scala-compiler" % scalaVersion % Provided)
      )
  }
}

lazy val surface =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-surface"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-surface",
      description := "A library for extracting object structure surface",
      // TODO: This is a temporary solution. Use AirSpec after Scala 3 support of Surface is completed
      libraryDependencies ++= surfaceDependencies(scalaVersion.value)
    )
    .jvmSettings(
      // For adding PreDestroy, PostConstruct annotations to Java9
      libraryDependencies += "javax.annotation" % "javax.annotation-api" % JAVAX_ANNOTATION_API_VERSION % Test
    )
    .jsSettings(jsBuildSettings)
    .nativeSettings(nativeBuildSettings)
    .dependsOn(log)

lazy val canvas =
  project
    .in(file("airframe-canvas"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-canvas",
      description := "Airframe off-heap memory library"
    )
    .dependsOn(log.jvm, control.jvm % Test)

lazy val config =
  project
    .in(file("airframe-config"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-config",
      description := "airframe configuration module",
      libraryDependencies ++= Seq(
        "org.yaml" % "snakeyaml" % SNAKE_YAML_VERSION
      )
    )
    .dependsOn(di.jvm, codec.jvm)

lazy val control =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-control"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-control",
      description := "A library for controlling program flows and retrying"
    )
    .jsSettings(jsBuildSettings)
    .nativeSettings(nativeBuildSettings)
    .dependsOn(log, rx)

lazy val ulid =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-ulid"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-ulid",
      description := "ULID: Universally Unique Lexicographically Sortable Identifier"
    )
    .jsSettings(
      jsBuildSettings,
      // For using SecureRandom (requires `crypto` package)
      libraryDependencies += ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13)
    )
    .nativeSettings(nativeBuildSettings)
    .dependsOn(log % Test)

lazy val jmx =
  project
    .in(file("airframe-jmx"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-jmx",
      description := "A library for exposing Scala object data through JMX",
      // Do not run tests concurrently to avoid JMX registration failures
      runTestSequentially
    )
    .dependsOn(surface.jvm)

lazy val launcher =
  project
    .in(file("airframe-launcher"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-launcher",
      description := "Command-line program launcher"
    )
    .dependsOn(surface.jvm, control.jvm, codec.jvm)

val logDependencies = { scalaVersion: String =>
  scalaVersion match {
    case s if s.startsWith("3.") =>
      Seq.empty
    case _ =>
      Seq("org.scala-lang" % "scala-reflect" % scalaVersion % Provided)
  }
}

val logJVMDependencies = Seq(
  // For rotating log files
  "ch.qos.logback" % "logback-core" % "1.5.8"
)

// airframe-log should have minimum dependencies
lazy val log: sbtcrossproject.CrossProject =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-log"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-log",
      description := "Fancy logger for Scala",
      scalacOptions ++= {
        if (scalaVersion.value.startsWith("3.")) Seq("-source:3.0-migration")
        else Nil
      },
      libraryDependencies ++= logDependencies(scalaVersion.value)
    )
    .jvmSettings(
      libraryDependencies ++= logJVMDependencies,
      runTestSequentially
    )
    .jsSettings(
      jsBuildSettings,
      libraryDependencies ++= Seq(
        ("org.scala-js" %%% "scalajs-java-logging" % JS_JAVA_LOGGING_VERSION).cross(CrossVersion.for3Use2_13)
      )
    )
    .nativeSettings(
      nativeBuildSettings
    )

lazy val metrics =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-metrics"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-metrics",
      description := "Basit metric representations, including duration, size, time window, etc."
    )
    .jsSettings(jsBuildSettings)
    .nativeSettings(nativeBuildSettings)
    .dependsOn(log, surface)

lazy val msgpack =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-msgpack"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-msgpack",
      description := "Pure-Scala MessagePack library"
    )
    .jvmSettings(
      libraryDependencies += "org.msgpack" % "msgpack-core" % MSGPACK_VERSION
    )
    .jsSettings(
      jsBuildSettings,
      libraryDependencies +=
        ("org.scala-js" %%% "scalajs-java-time" % JS_JAVA_TIME_VERSION).cross(CrossVersion.for3Use2_13)
    )
    .nativeSettings(
      nativeBuildSettings,
      // For using java.time libraries
      libraryDependencies += "org.ekrich" %%% "sjavatime" % "1.3.0"
    )
    .dependsOn(log, json)

lazy val codec =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-codec"))
    .settings(buildSettings)
    .settings(
      // TODO: #1698 Avoid "illegal multithreaded access to ContextBase error" on Scala 3
      // Tests in this project are sequentially executed
      // Test / parallelExecution := false,
      name        := "airframe-codec",
      description := "Airframe MessagePack-based codec"
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        // For JDBC testing
        "org.xerial" % "sqlite-jdbc" % SQLITE_JDBC_VERSION % Test
      )
    )
    .jsSettings(
      jsBuildSettings
    )
    .nativeSettings(nativeBuildSettings)
    .dependsOn(log, surface, msgpack, metrics, json, control, ulid)

lazy val jdbc =
  project
    .in(file("airframe-jdbc"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-jdbc",
      description := "JDBC connection pool service",
      libraryDependencies ++= Seq(
        "org.xerial"     % "sqlite-jdbc" % SQLITE_JDBC_VERSION,
        "org.duckdb"     % "duckdb_jdbc" % "1.4.3.0",
        "org.postgresql" % "postgresql"  % "42.7.8",
        "com.zaxxer"     % "HikariCP"    % "7.0.2",
        // For routing slf4j log to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(di.jvm, control.jvm, config)

lazy val rx =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-rx"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-rx",
      description := "Reactive stream (Rx) interface"
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "javax.annotation" % "javax.annotation-api" % JAVAX_ANNOTATION_API_VERSION % Test
      )
    )
    .jsSettings(
      jsBuildSettings,
      // For addressing the fairness issue of the global ExecutorContext https://github.com/scala-js/scala-js/issues/4129
      libraryDependencies += "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1"
    )
    .nativeSettings(nativeBuildSettings)
    .dependsOn(log)

lazy val http =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .enablePlugins(BuildInfoPlugin)
    .in(file("airframe-http"))
    .settings(buildSettings)
    .settings(
      name             := "airframe-http",
      description      := "REST and RPC Framework",
      buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "wvlet.airframe.http",
      buildInfoObject  := "AirframeHttpBuildInfo"
    )
    .jvmSettings(
      libraryDependencies += "javax.annotation" % "javax.annotation-api" % JAVAX_ANNOTATION_API_VERSION % Test,
      libraryDependencies ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, major)) if major <= 12 =>
            Seq()
          case _ =>
            Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0")
        }
      }
    )
    .jsSettings(
      jsBuildSettings,
      Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % SCALAJS_DOM_VERSION
      )
    )
    .nativeSettings(
      nativeBuildSettings
    )
    .dependsOn(rx, control, surface, json, codec, di)

lazy val httpCodeGen =
  project
    .in(file("airframe-http-codegen"))
    .enablePlugins(PackPlugin)
    .settings(buildSettings)
    .settings(
      name               := "airframe-http-codegen",
      description        := "REST and RPC code generator",
      packMain           := Map("airframe-http-code-generator" -> "wvlet.airframe.http.codegen.HttpCodeGenerator"),
      packExcludeLibJars := Seq("airspec_2.12", "airspec_2.13", "airspec_3"),
      packJvmVersionSpecificOpts := Map(
        "airframe-http-code-generator" -> Map(
          24 -> java24PlusJvmOptions
        )
      ),
      libraryDependencies ++= Seq(
        // Use swagger-parser only for validating YAML format in tests
        "io.swagger.parser.v3" % "swagger-parser" % "2.1.36" % Test,
        // Swagger includes dependency to SLF4J, so redirect slf4j logs to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION % Test,
        // For gRPC route scanner test
        "io.grpc" % "grpc-stub" % GRPC_VERSION % Test
      ),
      // Published package is necessary for sbt-airframe
      publishPackArchiveTgz
    )
    .dependsOn(http.jvm, launcher)

lazy val netty =
  project
    .in(file("airframe-http-netty"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-http-netty",
      description := "Airframe HTTP Netty backend",
      libraryDependencies ++= Seq(
        "io.netty" % "netty-all" % "4.2.7.Final"
      )
    )
    .dependsOn(http.jvm, rx.jvm)

lazy val grpc =
  project
    .in(file("airframe-http-grpc"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-http-grpc",
      description := "Airframe HTTP gRPC backend",
      libraryDependencies ++= Seq(
        "io.grpc"           % "grpc-netty-shaded" % GRPC_VERSION,
        "io.grpc"           % "grpc-stub"         % GRPC_VERSION,
        "org.apache.tomcat" % "annotations-api"   % "6.0.53"      % Provided,
        "org.slf4j"         % "slf4j-jdk14"       % SLF4J_VERSION % Test
      )
    )
    .dependsOn(http.jvm, rx.jvm)

// Workaround for com.twitter:util-core_2.12:21.4.0 (depends on 1.1.2)
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "always"

lazy val finagle =
  project
    .in(file("airframe-http-finagle"))
    .settings(buildSettings)
    .settings(scala2Only)
    .settings(
      name        := "airframe-http-finagle",
      description := "REST API binding for Finagle",
      // Finagle doesn't support Scala 2.13 yet
      libraryDependencies ++= Seq(
        ("com.twitter" %% "finagle-http"        % FINAGLE_VERSION).cross(CrossVersion.for3Use2_13),
        ("com.twitter" %% "finagle-netty4-http" % FINAGLE_VERSION).cross(CrossVersion.for3Use2_13),
        ("com.twitter" %% "finagle-netty4"      % FINAGLE_VERSION).cross(CrossVersion.for3Use2_13),
        ("com.twitter" %% "finagle-core"        % FINAGLE_VERSION).cross(CrossVersion.for3Use2_13),
        // Redirecting slf4j log in Finagle to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION,
        // Use a version that fixes [CVE-2017-18640]
        "org.yaml" % "snakeyaml" % SNAKE_YAML_VERSION
      )
    )
    .dependsOn(http.jvm)

lazy val okhttp =
  project
    .in(file("airframe-http-okhttp"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-http-okhttp",
      description := "REST API binding for OkHttp",
      libraryDependencies ++= Seq(
        "com.squareup.okhttp3" % "okhttp-jvm" % "5.3.2"
      )
    )
    .dependsOn(http.jvm, netty % Test)

lazy val httpRecorder =
  project
    .in(file("airframe-http-recorder"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-http-recorder",
      description := "Http Response Recorder",
      libraryDependencies ++= Seq(
      )
    )
    .dependsOn(codec.jvm, metrics.jvm, control.jvm, netty, jdbc)

lazy val json =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-json"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-json",
      description := "JSON parser"
    )
    .jsSettings(jsBuildSettings)
    .nativeSettings(nativeBuildSettings)
    .dependsOn(log)

lazy val benchmark =
  project
    .in(file("airframe-benchmark"))
    // Necessary for generating /META-INF/BenchmarkList
    .enablePlugins(JmhPlugin, PackPlugin)
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      crossScalaVersions := targetScalaVersions,
      name               := "airframe-benchmark",
      packMain           := Map("airframe-benchmark" -> "wvlet.airframe.benchmark.BenchmarkMain"),
      packJvmVersionSpecificOpts := Map(
        "airframe-benchmark" -> Map(
          24 -> java24PlusJvmOptions
        )
      ),
      // Turbo mode didn't work with this error:
      // java.lang.RuntimeException: ERROR: Unable to find the resource: /META-INF/BenchmarkList
      turbo := false,
      // Generate JMH benchmark cord before packaging and testing
      Compile / pack        := (Compile / pack).dependsOn(Test / compile).value,
      Jmh / sourceDirectory := (Compile / sourceDirectory).value,
      Jmh / compile         := (Jmh / compile).triggeredBy(Compile / compile).value,
      Test / compile        := ((Test / compile).dependsOn(Jmh / compile)).value,
      // Need to fork JVM so that sbt can set the classpass properly for running JMH
      run / fork := true,
      libraryDependencies ++= Seq(
        "org.msgpack"     % "msgpack-core"             % MSGPACK_VERSION,
        "org.openjdk.jmh" % "jmh-core"                 % JMH_VERSION,
        "org.openjdk.jmh" % "jmh-generator-bytecode"   % JMH_VERSION,
        "org.openjdk.jmh" % "jmh-generator-reflection" % JMH_VERSION,
        // Used only for json benchmark
        "io.github.json4s" %% "json4s-jackson" % "4.1.0",
        "io.circe"         %% "circe-parser"   % "0.14.15",
        // For ScalaPB
        // "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
        // For grpc-java
        "io.grpc"             % "grpc-protobuf" % GRPC_VERSION,
        "com.google.protobuf" % "protobuf-java" % "3.25.8",
        ("com.chatwork"      %% "scala-ulid"    % "1.0.24").cross(CrossVersion.for3Use2_13)
      )
      //      Compile / PB.targets := Seq(
      //        scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
      //      ),
      // publishing .tgz
      // publishPackArchiveTgz
    )
    .dependsOn(msgpack.jvm, json.jvm, metrics.jvm, launcher, httpCodeGen, netty, grpc, ulid.jvm)

lazy val fluentd =
  project
    .in(file("airframe-fluentd"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-fluentd",
      description := "Fluentd logger",
      libraryDependencies ++= Seq(
        "org.komamitsu" % "fluency-core"         % FLUENCY_VERSION,
        "org.komamitsu" % "fluency-fluentd"      % FLUENCY_VERSION,
        "org.komamitsu" % "fluency-treasuredata" % FLUENCY_VERSION
        // td-client-java -> json-simple happened to include junit 4.10 [CVE-2020-15250]
          exclude ("junit", "junit"),
        // Necessary for td-client-java, which is used in fluency-treasuredata
        "com.fasterxml.jackson.datatype" % "jackson-datatype-json-org" % "2.18.5" % Provided,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"     % "2.18.5" % Provided,
        // Redirecting slf4j log from Fluency to aiframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(codec.jvm, di.jvm)

def sqlRefLib = { scalaVersion: String =>
  if (scalaVersion.startsWith("2.13")) {
    Seq(
      // Include Spark just as a reference implementation
      "org.apache.spark" %% "spark-sql" % "4.1.0" % Test,
      // Include Trino as a reference implementation
      "io.trino" % "trino-main" % "479" % Test
    )
  } else {
    Seq.empty
  }
}

lazy val parquet =
  project
    .in(file("airframe-parquet"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-parquet",
      description := "Parquet columnar format reader/writer support (Hadoop-free using LocalInputFile/LocalOutputFile)",
      libraryDependencies ++= Seq(
        // Use parquet-hadoop with NioInputFile/LocalOutputFile for Hadoop-free operation
        "org.apache.parquet" % "parquet-hadoop" % PARQUET_VERSION,
        // This approach is based on the technique described at:
        // https://blakesmith.me/2024/10/05/how-to-use-parquet-java-without-hadoop.html
        //
        // It relies on providing just enough of the Hadoop classes for parquet-hadoop to work with
        // NioInputFile/LocalOutputFile for local and remote NIO filesystem I/O, while excluding all
        // of their transitive dependencies. This achieves an 85%+ reduction in dependency size.
        //
        // Note: This is fragile and might break with future parquet-hadoop updates that introduce
        // new Hadoop dependencies. If compilation fails after upgrading parquet-hadoop, check for
        // new NoClassDefFoundError exceptions and add the missing dependencies here with exclusions.
        ("org.apache.hadoop" % "hadoop-common" % "3.4.2")
          .excludeAll(ExclusionRule(organization = "*")),
        ("com.fasterxml.woodstox" % "woodstox-core" % "7.1.1")
          .excludeAll(ExclusionRule(organization = "*")),
        ("org.codehaus.woodstox" % "stax2-api" % "4.2.2")
          .excludeAll(ExclusionRule(organization = "*")),
        ("commons-collections" % "commons-collections" % "3.2.2")
          .excludeAll(ExclusionRule(organization = "*")),
        ("org.apache.commons" % "commons-collections4" % "4.5.0")
          .excludeAll(ExclusionRule(organization = "*")),
        ("org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.4.2")
          .excludeAll(ExclusionRule(organization = "*")),
        ("org.apache.hadoop.thirdparty" % "hadoop-shaded-guava" % "1.5.0")
          .excludeAll(ExclusionRule(organization = "*")),
        // For Apple Silicon (M1)
        "org.xerial.snappy"  % "snappy-java"  % "1.1.10.8",
        "org.slf4j"          % "slf4j-jdk14"  % SLF4J_VERSION   % Optional,
        "org.apache.parquet" % "parquet-avro" % PARQUET_VERSION % Test
      ),
      Test / fork := true
    )
    .dependsOn(codec.jvm, sql)

lazy val sql =
  project
    .enablePlugins(Antlr4Plugin)
    .in(file("airframe-sql"))
    .settings(buildSettings)
    .settings(
      name                       := "airframe-sql",
      description                := "SQL parser & analyzer",
      Antlr4 / antlr4Version     := "4.13.2",
      Antlr4 / antlr4PackageName := Some("wvlet.airframe.sql.parser"),
      Antlr4 / antlr4GenListener := true,
      Antlr4 / antlr4GenVisitor  := true,
      libraryDependencies ++= Seq(
        // For parsing DataType strings
        "org.scala-lang.modules"   %% "scala-parser-combinators" % SCALA_PARSER_COMBINATOR_VERSION,
        "com.github.vertical-blank" % "sql-formatter"            % "2.0.5"
      ) ++ sqlRefLib(scalaVersion.value)
    )
    .dependsOn(msgpack.jvm, surface.jvm, config, launcher)

lazy val rxHtml =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-rx-html"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-rx-html",
      description := "Reactive HTML elements for Scala and Scala.js",
      libraryDependencies ++= {
        if (scalaVersion.value.startsWith("3."))
          Seq.empty
        else
          Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided)
      }
    )
    .jsSettings(
      jsBuildSettings,
      Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % SCALAJS_DOM_VERSION
      )
    )
    .dependsOn(log, rx, surface)

lazy val widgetJS =
  project
    .enablePlugins(ScalaJSPlugin) // , ScalaJSBundlerPlugin)
    .in(file("airframe-rx-widget"))
    .settings(buildSettings)
    .settings(
      name        := "airframe-rx-widget",
      description := "Reactive Widget library for Scala.js",
      jsBuildSettings,
      Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
      // npmDependencies in Compile += "monaco-editor" -> "0.21.3",
      // useYarn := true
      //      npmDependencies in Test += "node" -> "12.14.1"
    )
    .dependsOn(log.js, rxHtml.js)

lazy val examples =
  project
    .in(file("examples"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      name               := "airframe-examples",
      description        := "Airframe examples",
      crossScalaVersions := targetScalaVersions,
      libraryDependencies ++= Seq(
      )
    )
    .dependsOn(
      codec.jvm,
      config,
      di.jvm,
      diMacros.jvm,
      launcher,
      jmx,
      jdbc,
      netty
    )

// Dotty test project
lazy val dottyTest =
  project
    .in(file("airframe-dotty-test"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      name               := "airframe-dotty-test",
      description        := "test for dotty",
      scalaVersion       := SCALA_3,
      crossScalaVersions := List(SCALA_3)
    )
    .dependsOn(log.jvm, surface.jvm, di.jvm, codec.jvm)

// Integration test for Scala 3
lazy val integrationTestApi =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-integration-test-api"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      scala3Only,
      // Skip importing the integration test projects in IntelliJ IDEA due the the following error:
      // [error] Modules were resolved with conflicting cross-version suffixes in ProjectRef(uri("file:/Users/leo/work/airframe/"), "integrationTestApiJS"):
      // [error]    org.scala-js:scala-js-macrotask-executor_sjs1 _3, _2.13
      ideSkipProject := true,
      name           := "airframe-integration-test-api",
      description    := "APIs for integration test"
    )
    .dependsOn(http)

// Integration test for Scala 3
lazy val integrationTest =
  project
    .in(file("airframe-integration-test"))
    .enablePlugins(AirframeHttpPlugin)
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      scala3Only,
      ideSkipProject      := true,
      name                := "airframe-integration-test",
      description         := "integration test project",
      airframeHttpClients := Seq("wvlet.airframe.test.api:rpc")
    )
    .dependsOn(integrationTestApi.jvm, netty)

lazy val integrationTestJs =
  project
    .enablePlugins(ScalaJSPlugin)
    .in(file("airframe-integration-test-js"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      scala3Only,
      ideSkipProject := true,
      name           := "airframe-integration-test-js",
      description    := "browser integration test for Scala.js",
      Test / jsEnv := new jsenv.playwright.PWEnv(
        browserName = "chrome",
        headless = true,
        showLogs = false
      )
    )
    .dependsOn(integrationTestApi.js)
