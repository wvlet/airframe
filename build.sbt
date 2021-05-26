import sbt.Keys.libraryDependencies
import xerial.sbt.pack.PackPlugin.publishPackArchiveTgz

val SCALA_2_12          = "2.12.13"
val SCALA_2_13          = "2.13.6"
val SCALA_3_0           = "3.0.0"
val targetScalaVersions = SCALA_2_13 :: SCALA_2_12 :: Nil
val withDotty           = SCALA_3_0 :: targetScalaVersions

val AIRSPEC_VERSION                 = "21.5.4"
val SCALACHECK_VERSION              = "1.15.4"
val MSGPACK_VERSION                 = "0.8.24"
val SCALA_PARSER_COMBINATOR_VERSION = "2.0.0"
val SQLITE_JDBC_VERSION             = "3.34.0"
val SLF4J_VERSION                   = "1.7.30"
val JS_JAVA_LOGGING_VERSION         = "1.0.0"
val JS_JAVA_TIME_VERSION            = "1.0.0"
val SCALAJS_DOM_VERSION             = "1.1.0"
val FINAGLE_VERSION                 = "21.4.0"
val FLUENCY_VERSION                 = "2.5.1"
val GRPC_VERSION                    = "1.38.0"
val JMH_VERSION                     = "1.31"
val JAVAX_ANNOTATION_API_VERSION    = "1.3.2"
val PARQUET_VERSION                 = "1.12.0"

// A short cut for publishing snapshots to Sonatype
addCommandAlias(
  "publishSnapshots",
  s"+ projectJVM/publish; + projectJS/publish"
)

// [Development purpose] publish all artifacts to the local repo
addCommandAlias(
  "publishAllLocal",
  s"+ projectJVM/publishLocal; + projectJS/publishLocal;"
)

addCommandAlias(
  "publishJSSigned",
  s"+ projectJS/publishSigned"
)
addCommandAlias(
  "publishJSLocal",
  s"+ projectJS/publishLocal"
)

// Allow using Ctrl+C in sbt without exiting the prompt
// Global / cancelable := true

//ThisBuild / turbo := true

// Reload build.sbt on changes
Global / onChangedBuildSource := ReloadOnSourceChanges

// Disable the pipelining available since sbt-1.4.0. It caused compilation failure
ThisBuild / usePipelining := false

// A build configuration switch for working on Dotty migration. This needs to be removed eventually
val DOTTY = sys.env.isDefinedAt("DOTTY")
// For debugging
// val DOTTY = true

// We MUST use Scala 2.12 for building sbt-airframe
ThisBuild / scalaVersion := {
  if (DOTTY) SCALA_3_0
  else SCALA_2_13
}
ThisBuild / organization := "org.wvlet.airframe"

// Use dynamic snapshot version strings for non tagged versions
ThisBuild / dynverSonatypeSnapshots := true
// Use coursier friendly version separator
ThisBuild / dynverSeparator := "-"

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
  // Exclude compile-time only projects. This is a workaround for bloop,
  // which cannot resolve Optional dependencies nor compile-internal dependencies.
  pomPostProcess := excludePomDependency(Seq("airspec_2.12", "airspec_2.13")),
  crossScalaVersions := targetScalaVersions,
  crossPaths := true,
  publishMavenStyle := true,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation"
  ) ++ {
    if (DOTTY) {
      Seq.empty
    } else {
      Seq(
        // Necessary for tracking source code range in airframe-rx demo
        "-Yrangepos"
      )
    }
  },
  testFrameworks += new TestFramework("wvlet.airspec.Framework"),
  libraryDependencies ++= {
    if (DOTTY)
      Seq.empty
    else
      Seq("org.scala-lang.modules" %%% "scala-collection-compat" % "2.4.4")
  }
)

// Do not run tests concurrently to avoid JMX registration failures
val runTestSequentially = Seq[Setting[_]](Test / parallelExecution := false)

// We need to define this globally as a workaround for https://github.com/sbt/sbt/pull/3760
ThisBuild / publishTo := sonatypePublishToBundle.value

val jsBuildSettings = Seq[Setting[_]](
  crossScalaVersions := targetScalaVersions,
  coverageEnabled := false
)

val noPublish = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  // Explicitely skip the doc task because protobuf related Java files causes no type found error
  Compile / doc / sources := Seq.empty,
  Compile / packageDoc / publishArtifact := false
)

Global / excludeLintKeys ++= Set(sonatypeProfileName, sonatypeSessionName)

lazy val root =
  project
    .in(file("."))
    .settings(name := "airframe-root")
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      sonatypeProfileName := "org.wvlet",
      sonatypeSessionName := {
        if (sys.env.isDefinedAt("SCALAJS")) {
          // Use a different session for Scala.js projects
          s"${sonatypeSessionName.value} for Scala.js"
        } else {
          sonatypeSessionName.value
        }
      }
    )
    .aggregate((jvmProjects ++ jsProjects): _*)

// JVM projects for scala-community build. This should have no tricky setup and should support Scala 2.12.
lazy val communityBuildProjects: Seq[ProjectReference] = Seq(
  diMacrosJVM,
  diJVM,
  surfaceJVM,
  logJVM,
  canvas,
  config,
  controlJVM,
  ulidJVM,
  jmx,
  launcher,
  metricsJVM,
  codecJVM,
  msgpackJVM,
  rxJVM,
  httpJVM,
  httpRouter,
  httpCodeGen,
  grpc,
  jsonJVM,
  rxHtmlJVM,
  parquet,
  airspecJVM
)

// Other JVM projects supporting Scala 2.12 - Scala 2.13
lazy val jvmProjects: Seq[ProjectReference] = communityBuildProjects ++ Seq[ProjectReference](
  jdbc,
  fluentd,
  finagle,
  okhttp,
  httpRecorder,
  benchmark,
  sql,
  examples
)

// Scala.js build (only for Scala 2.12 + 2.13)
lazy val jsProjects: Seq[ProjectReference] = Seq(
  logJS,
  surfaceJS,
  diMacrosJS,
  diJS,
  metricsJS,
  controlJS,
  ulidJS,
  jsonJS,
  msgpackJS,
  codecJS,
  rxJS,
  httpJS,
  rxHtmlJS,
  widgetJS,
  airspecJS
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
    .aggregate(jvmProjects: _*)

lazy val projectJS =
  project
    .settings(
      noPublish,
      crossScalaVersions := targetScalaVersions
    )
    .aggregate(jsProjects: _*)

// For Dotty (Scala 3)
lazy val projectDotty =
  project
    .settings(
      noPublish,
      crossScalaVersions := Seq(SCALA_3_0)
    )
    .aggregate(
      diMacrosJVM,
      diJVM,
      airspecJVM,
      logJVM,
      surfaceJVM,
      canvas,
      controlJVM,
      // codec uses Scala reflection
      codecJVM,
      //fluentd,
      //httpJVM,
      //// Finagle isn't supporting Scala 3
      //httpFinagle,
      //grpc,
      //jdbc,
      //jmx,
      //launcher,
      metricsJVM,
      msgpackJVM,
      jsonJVM,
      rxJVM,
      // rx-html uses Scala Macros
      //rxHtmlJVM,
      //sql,
      ulidJVM
    )

lazy val docs =
  project
    .in(file("airframe-docs"))
    .settings(
      name := "airframe-docs",
      moduleName := "airframe-docs",
      publishArtifact := false,
      publish := {},
      publishLocal := {},
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

import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
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

def airframeDIDependencies = Seq(
  "javax.annotation" % "javax.annotation-api" % JAVAX_ANNOTATION_API_VERSION
)

lazy val di =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-di"))
    .settings(buildSettings)
    .settings(dottyCrossBuildSettings)
    .settings(
      name := "airframe",
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
    .dependsOn(
      surface,
      diMacros,
      airspecRef % Test
    )

lazy val diJVM = di.jvm
lazy val diJS  = di.js

def crossBuildSources(scalaBinaryVersion: String, baseDir: String, srcType: String = "main"): Seq[sbt.File] = {
  val scalaMajorVersion = scalaBinaryVersion.split("\\.").head
  for (suffix <- Seq("", s"-${scalaBinaryVersion}", s"-${scalaMajorVersion}").distinct)
    yield {
      file(s"${baseDir}/src/${srcType}/scala${suffix}")
    }
}

def dottyCrossBuildSettings: Seq[Setting[_]] = {
  Seq(
    crossScalaVersions := {
      if (DOTTY) withDotty else targetScalaVersions
    },
    Compile / unmanagedSourceDirectories := {
      val origDirs = (Compile / unmanagedSourceDirectories).value
      val newDirs = crossBuildSources(
        scalaBinaryVersion.value,
        baseDirectory.value.getParent
      )
      (origDirs ++ newDirs).distinct
    },
    Test / unmanagedSourceDirectories := {
      val origDirs = (Test / unmanagedSourceDirectories).value
      val newDirs = crossBuildSources(
        scalaBinaryVersion.value,
        baseDirectory.value.getParent,
        srcType = "test"
      )
      (origDirs ++ newDirs).distinct
    }
  )
}

// Airframe DI needs to call macro methods, so we needed to split the project into DI and DI macros.
// This project sources and classes will be embedded to airframe.jar, so we don't publish airframe-di-macros
lazy val diMacros =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-di-macros"))
    .settings(buildSettings)
    .settings(dottyCrossBuildSettings)
    .settings(
      name := "airframe-di-macros",
      description := "Macros for Airframe Di"
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log, surface, airspecRef % Test)

lazy val diMacrosJVM = diMacros.jvm
lazy val diMacrosJS  = diMacros.js

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

val surfaceJVMDependencies = { scalaVersion: String =>
  scalaVersion match {
    case s if s.startsWith("3.") =>
      Seq(
        "org.scala-lang" %% "scala3-tasty-inspector" % s,
        "org.scala-lang" %% "scala3-staging"         % s
      )
    case _ => Seq.empty
  }
}

lazy val surface =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-surface"))
    .settings(buildSettings)
    .settings(dottyCrossBuildSettings)
    .settings(
      name := "airframe-surface",
      description := "A library for extracting object structure surface",
      libraryDependencies ++= surfaceDependencies(scalaVersion.value)
    )
    .jvmSettings(
      // For adding PreDestroy, PostConstruct annotations to Java9
      libraryDependencies ++= surfaceJVMDependencies(scalaVersion.value),
      libraryDependencies += "javax.annotation" % "javax.annotation-api" % JAVAX_ANNOTATION_API_VERSION % Test
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log, airspecRef % Test)

lazy val surfaceJVM = surface.jvm
lazy val surfaceJS  = surface.js

lazy val canvas =
  project
    .in(file("airframe-canvas"))
    .settings(buildSettings)
    .settings(
      name := "airframe-canvas",
      description := "Airframe off-heap memory library"
    )
    .dependsOn(logJVM, controlJVM % Test, airspecRefJVM % Test)

lazy val config =
  project
    .in(file("airframe-config"))
    .settings(buildSettings)
    .settings(
      name := "airframe-config",
      description := "airframe configuration module",
      libraryDependencies ++= Seq(
        "org.yaml" % "snakeyaml" % "1.28"
      )
    )
    .dependsOn(diJVM, codecJVM, airspecRefJVM % Test)

lazy val control =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-control"))
    .settings(buildSettings)
    .settings(
      name := "airframe-control",
      description := "A library for controlling program flows and retrying"
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % SCALA_PARSER_COMBINATOR_VERSION
      )
    )
    .dependsOn(log, airspecRef % Test)

lazy val controlJVM = control.jvm
lazy val controlJS  = control.js

lazy val ulid =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-ulid"))
    .settings(buildSettings)
    .settings(
      name := "airframe-ulid",
      description := "ULID: Universally Unique Lexicographically Sortable Identifier"
    )
    .jsSettings(
      jsBuildSettings
    )
    .dependsOn(log % Test, airspecRef % Test)

lazy val ulidJVM = ulid.jvm
lazy val ulidJS  = ulid.js

lazy val jmx =
  project
    .in(file("airframe-jmx"))
    .settings(buildSettings)
    .settings(
      name := "airframe-jmx",
      description := "A library for exposing Scala object data through JMX",
      // Do not run tests concurrently to avoid JMX registration failures
      runTestSequentially
    )
    .dependsOn(surfaceJVM, airspecRefJVM % Test)

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
    .dependsOn(surfaceJVM, controlJVM, codecJVM, airspecRefJVM % Test)

val logDependencies = { scalaVersion: String =>
  scalaVersion match {
    case s if DOTTY =>
      Seq.empty
    case _ =>
      Seq("org.scala-lang" % "scala-reflect" % scalaVersion % Provided)
  }
}

val logJVMDependencies = Seq(
  "ch.qos.logback" % "logback-core" % "1.2.3"
)

// airframe-log should have minimum dependencies
lazy val log: sbtcrossproject.CrossProject =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-log"))
    .settings(buildSettings)
    .settings(dottyCrossBuildSettings)
    .settings(
      name := "airframe-log",
      description := "Fancy logger for Scala",
      scalacOptions ++= {
        if (DOTTY) Seq("-source:3.0-migration")
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
    .dependsOn(airspecRef % Test)

lazy val logJVM = log.jvm
lazy val logJS  = log.js

lazy val metrics =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-metrics"))
    .settings(buildSettings)
    .settings(
      name := "airframe-metrics",
      description := "Basit metric representations, including duration, size, time window, etc."
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(log, surface, airspecRef % Test)

lazy val metricsJVM = metrics.jvm
lazy val metricsJS  = metrics.js

lazy val msgpack =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-msgpack"))
    .settings(buildSettings)
    .settings(
      name := "airframe-msgpack",
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
    .dependsOn(log, json, airspecRef % Test)

lazy val msgpackJVM = msgpack.jvm
lazy val msgpackJS  = msgpack.js

lazy val codec =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-codec"))
    .settings(buildSettings)
    .settings(dottyCrossBuildSettings)
    .settings(
      name := "airframe-codec",
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
    .dependsOn(log, surface, msgpack, metrics, json, control, ulid, airspecRef % Test)

lazy val codecJVM = codec.jvm
lazy val codecJS  = codec.js

lazy val jdbc =
  project
    .in(file("airframe-jdbc"))
    .settings(buildSettings)
    .settings(
      name := "airframe-jdbc",
      description := "JDBC connection pool service",
      libraryDependencies ++= Seq(
        "org.xerial"     % "sqlite-jdbc" % SQLITE_JDBC_VERSION,
        "org.postgresql" % "postgresql"  % "42.2.20",
        "com.zaxxer"     % "HikariCP"    % "4.0.3",
        // For routing slf4j log to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(diJVM, controlJVM, config, airspecRefJVM % Test)

lazy val rx =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-rx"))
    .settings(buildSettings)
    .settings(
      name := "airframe-rx",
      description := "Reactive stream (Rx) interface"
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "javax.annotation" % "javax.annotation-api" % JAVAX_ANNOTATION_API_VERSION % Test
      )
    )
    .jsSettings(
      jsBuildSettings
    )
    .dependsOn(log, airspecRef % Test)

lazy val rxJVM = rx.jvm
lazy val rxJS  = rx.js

lazy val http =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .enablePlugins(BuildInfoPlugin)
    .in(file("airframe-http"))
    .settings(buildSettings)
    .settings(dottyCrossBuildSettings)
    .settings(
      name := "airframe-http",
      description := "REST and RPC Framework",
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "wvlet.airframe.http"
    )
    .jvmSettings(
      libraryDependencies += "javax.annotation" % "javax.annotation-api" % JAVAX_ANNOTATION_API_VERSION % Test,
      libraryDependencies ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, major)) if major <= 12 =>
            Seq()
          case _ =>
            Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.3")
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
    .dependsOn(rx, control, surface, json, codec, airspecRef % Test)

lazy val httpJVM = http.jvm
lazy val httpJS  = http.js

lazy val httpRouter =
  project
    .in(file("airframe-http-router"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http-router",
      description := "Request routing library"
    )
    .dependsOn(diJVM, httpJVM, airspecRefJVM % Test)

lazy val httpCodeGen =
  project
    .in(file("airframe-http-codegen"))
    .enablePlugins(PackPlugin)
    .settings(buildSettings)
    .settings(
      name := "airframe-http-codegen",
      description := "REST and RPC code generator",
      packMain := Map("airframe-http-code-generator" -> "wvlet.airframe.http.codegen.HttpCodeGenerator"),
      packExcludeLibJars := Seq("airspec_2.12", "airspec_2.13"),
      libraryDependencies ++= Seq(
        // Use swagger-parser only for validating YAML format in tests
        "io.swagger.parser.v3" % "swagger-parser" % "2.0.25" % Test,
        // Swagger includes dependency to SLF4J, so redirect slf4j logs to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION % Test
      ),
      // Published package is necessary for sbt-airframe
      publishPackArchiveTgz
    )
    .dependsOn(httpRouter, launcher, airspecRefJVM % Test)

lazy val grpc =
  project
    .in(file("airframe-http-grpc"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http-grpc",
      description := "Airframe HTTP gRPC backend",
      libraryDependencies ++= Seq(
        "io.grpc"           % "grpc-netty-shaded" % GRPC_VERSION,
        "io.grpc"           % "grpc-stub"         % GRPC_VERSION,
        "org.apache.tomcat" % "annotations-api"   % "6.0.53"      % Provided,
        "org.slf4j"         % "slf4j-jdk14"       % SLF4J_VERSION % Test
      )
    )
    .dependsOn(httpRouter, rxJVM, airspecRefJVM % Test)

// Workaround for com.twitter:util-core_2.12:21.4.0 (depends on 1.1.2)
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "always"

lazy val finagle =
  project
    .in(file("airframe-http-finagle"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http-finagle",
      description := "REST API binding for Finagle",
      // Finagle doesn't support Scala 2.13 yet
      libraryDependencies ++= Seq(
        "com.twitter" %% "finagle-http"        % FINAGLE_VERSION,
        "com.twitter" %% "finagle-netty4-http" % FINAGLE_VERSION,
        "com.twitter" %% "finagle-netty4"      % FINAGLE_VERSION,
        "com.twitter" %% "finagle-core"        % FINAGLE_VERSION,
        // Redirecting slf4j log in Finagle to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(httpRouter, airspecRefJVM % Test)

lazy val okhttp =
  project
    .in(file("airframe-http-okhttp"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http-okhttp",
      description := "REST API binding for OkHttp",
      libraryDependencies ++= Seq(
        "com.squareup.okhttp3" % "okhttp" % "3.14.9"
      )
    )
    .dependsOn(httpJVM, finagle % Test, airspecRefJVM % Test)

lazy val httpRecorder =
  project
    .in(file("airframe-http-recorder"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http-recorder",
      description := "Http Response Recorder",
      // Finagle doesn't support Scala 2.13 yet
      libraryDependencies ++= Seq(
        "com.twitter" %% "finagle-netty4-http" % FINAGLE_VERSION,
        "com.twitter" %% "finagle-netty4"      % FINAGLE_VERSION,
        "com.twitter" %% "finagle-core"        % FINAGLE_VERSION,
        // Redirecting slf4j log in Finagle to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(codecJVM, metricsJVM, controlJVM, finagle, jdbc, airspecRefJVM % Test)

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
    .dependsOn(log, airspecRef % Test)

lazy val jsonJVM = json.jvm
lazy val jsonJS  = json.js

lazy val benchmark =
  project
    .in(file("airframe-benchmark"))
    // Necessary for generating /META-INF/BenchmarkList
    .enablePlugins(JmhPlugin, PackPlugin)
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      name := "airframe-benchmark",
      packMain := Map("airframe-benchmark" -> "wvlet.airframe.benchmark.BenchmarkMain"),
      // Turbo mode didn't work with this error:
      // java.lang.RuntimeException: ERROR: Unable to find the resource: /META-INF/BenchmarkList
      turbo := false,
      // Generate JMH benchmark cord before packaging and testing
      pack := pack.dependsOn(Test / compile).value,
      Jmh / sourceDirectory := (Compile / sourceDirectory).value,
      Jmh / compile := (Jmh / compile).triggeredBy(Compile / compile).value,
      Test / compile := ((Test / compile).dependsOn(Jmh / compile)).value,
      // Need to fork JVM so that sbt can set the classpass properly for running JMH
      run / fork := true,
      libraryDependencies ++= Seq(
        "org.msgpack"     % "msgpack-core"             % MSGPACK_VERSION,
        "org.openjdk.jmh" % "jmh-core"                 % JMH_VERSION,
        "org.openjdk.jmh" % "jmh-generator-bytecode"   % JMH_VERSION,
        "org.openjdk.jmh" % "jmh-generator-reflection" % JMH_VERSION,
        // Used only for json benchmark
        "org.json4s" %% "json4s-jackson" % "4.0.0",
        "io.circe"   %% "circe-parser"   % "0.13.0",
        // For ScalaPB
        // "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
        // For grpc-java
        "io.grpc"             % "grpc-protobuf" % GRPC_VERSION,
        "com.google.protobuf" % "protobuf-java" % "3.17.1",
        "com.chatwork"       %% "scala-ulid"    % "1.0.7"
      )
      //      Compile / PB.targets := Seq(
      //        scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
      //      ),
      // publishing .tgz
      // publishPackArchiveTgz
    )
    .dependsOn(msgpackJVM, jsonJVM, metricsJVM, launcher, httpCodeGen, finagle, grpc, ulidJVM, airspecRefJVM % Test)

lazy val fluentd =
  project
    .in(file("airframe-fluentd"))
    .settings(buildSettings)
    .settings(
      name := "airframe-fluentd",
      description := "Fluentd logger",
      libraryDependencies ++= Seq(
        "org.komamitsu" % "fluency-core"         % FLUENCY_VERSION,
        "org.komamitsu" % "fluency-fluentd"      % FLUENCY_VERSION,
        "org.komamitsu" % "fluency-treasuredata" % FLUENCY_VERSION,
        // Redirecting slf4j log from Fluency to aiframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(codecJVM, diJVM, airspecRefJVM % Test)

def sqlRefLib = { scalaVersion: String =>
  if (scalaVersion.startsWith("2.12")) {
    Seq(
      // Include Spark just as a reference implementation
      "org.apache.spark" %% "spark-sql" % "3.1.1" % Test,
      // Include Trino as a reference implementation
      "io.trino" % "trino-main" % "357" % Test
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
      name := "airframe-parquet",
      description := "Parquet columnar format reader/writer support",
      libraryDependencies ++= Seq(
        "org.apache.parquet" % "parquet-hadoop" % PARQUET_VERSION,
        "org.apache.hadoop"  % "hadoop-client"  % "3.3.0" % Provided,
        // For Apple Silicon (M1)
        "org.xerial.snappy"  % "snappy-java"  % "1.1.8.4",
        "org.slf4j"          % "slf4j-jdk14"  % SLF4J_VERSION   % Optional,
        "org.apache.parquet" % "parquet-avro" % PARQUET_VERSION % Test
      )
    )
    .dependsOn(codecJVM, sql, airspecRefJVM % Test)

lazy val sql =
  project
    .enablePlugins(Antlr4Plugin)
    .in(file("airframe-sql"))
    .settings(buildSettings)
    .settings(
      name := "airframe-sql",
      description := "SQL parser & analyzer",
      Antlr4 / antlr4Version := "4.9.2",
      Antlr4 / antlr4PackageName := Some("wvlet.airframe.sql.parser"),
      Antlr4 / antlr4GenListener := true,
      Antlr4 / antlr4GenVisitor := true,
      libraryDependencies ++= Seq(
        // For parsing DataType strings
        "org.scala-lang.modules" %% "scala-parser-combinators" % SCALA_PARSER_COMBINATOR_VERSION
      ) ++ sqlRefLib(scalaVersion.value)
    )
    .dependsOn(msgpackJVM, surfaceJVM, config, launcher, airspecRefJVM % Test)

lazy val rxHtml =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-rx-html"))
    .settings(buildSettings)
    .settings(
      name := "airframe-rx-html",
      description := "Reactive HTML elements for Scala and Scala.js",
      libraryDependencies ++= {
        if (DOTTY)
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
    .dependsOn(log, rx, surface, airspecRef % Test)

lazy val rxHtmlJVM = rxHtml.jvm
lazy val rxHtmlJS  = rxHtml.js

lazy val widgetJS =
  project
    .enablePlugins(ScalaJSPlugin) //, ScalaJSBundlerPlugin)
    .in(file("airframe-rx-widget"))
    .settings(buildSettings)
    .settings(
      name := "airframe-rx-widget",
      description := "Reactive Widget library for Scala.js",
      jsBuildSettings,
      Test / jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
      // npmDependencies in Compile += "monaco-editor" -> "0.21.3",
      // useYarn := true
      //      npmDependencies in Test += "node" -> "12.14.1"
    )
    .dependsOn(logJS, rxHtmlJS, airspecRefJS % Test)

lazy val examples =
  project
    .in(file("examples"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      name := "airframe-examples",
      description := "Airframe examples",
      libraryDependencies ++= Seq(
      )
    )
    .dependsOn(
      codecJVM,
      config,
      diJVM,
      diMacrosJVM,
      launcher,
      jmx,
      jdbc,
      finagle,
      airspecRefJVM % Test
    )

// Dotty test project
lazy val dottyTest =
  project
    .in(file("airframe-dotty-test"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      name := "airframe-dotty-test",
      description := "test for dotty",
      crossScalaVersions := {
        if (DOTTY) withDotty
        else targetScalaVersions
      }
    )
    .dependsOn(logJVM, surfaceJVM, diJVM, codecJVM, airspecRefJVM % Test)

/**
  * AirSpec build definitions.
  *
  * To make airspec a standalone library without any cyclic project references, airspec embeds the source code of airframe-log, di, surface, etc.
  *
  * Since airframe-log, di, and surfaces uses Scala macros whose def-macros cannot be called within the same project,
  * we need to split the source code into 4 projects:
  *
  *  - airspec-log (dependsOn airframe-log's source)
  *  - airspec-core (di-macros, surface)  # surface uses airframe-log macros
  *  - airspec-deps (di, metrics)  # di uses di-macros
  *  - airspec (test-interface) # Need to split this as IntelliJ cannot find classes in unmanagedSourceDirectories
  *
  * airspec.jar will be an all-in-one jar with airframe-log, di, surface, metrics, etc.
  */
val airspecLogDependencies  = Seq("airframe-log")
val airspecCoreDependencies = Seq("airframe-di-macros", "airframe-surface")
val airspecDependencies     = Seq("airframe-di", "airframe-metrics")

// Setting keys for AirSpec
val airspecDependsOn =
  settingKey[Seq[String]]("Dependent module names of airspec projects")

val airspecBuildSettings = Seq[Setting[_]](
  Compile / unmanagedSourceDirectories ++= {
    val baseDir = (ThisBuild / baseDirectory).value.getAbsoluteFile
    val sv      = scalaBinaryVersion.value
    val sourceDirs =
      for (m <- airspecDependsOn.value; infix <- Seq("")) yield {
        crossBuildSources(sv, s"${baseDir}/${m}${infix}")
      }
    sourceDirs.flatten
  }
)

val airspecJVMBuildSettings = Seq[Setting[_]](
  Compile / unmanagedSourceDirectories ++= {
    val baseDir = (ThisBuild / baseDirectory).value.getAbsoluteFile
    val sv      = scalaBinaryVersion.value
    val sourceDirs =
      for (m <- airspecDependsOn.value; folder <- Seq(".jvm")) yield {
        crossBuildSources(sv, s"${baseDir}/${m}/${folder}")
      }
    sourceDirs.flatten
  }
)

val airspecJSBuildSettings = Seq[Setting[_]](
  Compile / unmanagedSourceDirectories ++= {
    val baseDir = (ThisBuild / baseDirectory).value.getAbsoluteFile
    val sv      = scalaBinaryVersion.value
    val sourceDirs =
      for (m <- airspecDependsOn.value; folder <- Seq(".js")) yield {
        crossBuildSources(sv, s"${baseDir}/${m}/${folder}")
      }
    sourceDirs.flatten
  }
)

lazy val airspecLog =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("airspec-log"))
    .settings(buildSettings)
    .settings(noPublish)
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
        ("org.scala-js" %%% "scalajs-java-logging" % JS_JAVA_LOGGING_VERSION).cross(CrossVersion.for3Use2_13)
      )
    )

lazy val airspecLogJVM = airspecLog.jvm
lazy val airspecLogJS  = airspecLog.js

lazy val airspecCore =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("airspec-core"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      airspecDependsOn := airspecCoreDependencies,
      airspecBuildSettings,
      name := "airspec-core",
      description := "A core module of AirSpec with Surface and DI macros",
      libraryDependencies ++= surfaceDependencies(scalaVersion.value)
    )
    .jvmSettings(
      airspecJVMBuildSettings,
      libraryDependencies ++= surfaceJVMDependencies(scalaVersion.value),
      Compile / packageBin / mappings ++= (airspecLogJVM / Compile / packageBin / mappings).value,
      Compile / packageSrc / mappings ++= (airspecLogJVM / Compile / packageSrc / mappings).value
    )
    .jsSettings(
      airspecJSBuildSettings,
      Compile / packageBin / mappings ++= (airspecLogJS / Compile / packageBin / mappings).value
        .filter(x => x._2 != "JS_DEPENDENCIES"),
      Compile / packageSrc / mappings ++= (airspecLogJS / Compile / packageSrc / mappings).value
    )
    .dependsOn(airspecLog)

lazy val airspecCoreJVM = airspecCore.jvm
lazy val airspecCoreJS  = airspecCore.js

lazy val airspecDeps =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("airspec-deps"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      airspecDependsOn := airspecDependencies,
      airspecBuildSettings,
      name := "airspec-deps",
      description := "Dependencies of AirSpec"
    )
    .jvmSettings(
      airspecJVMBuildSettings,
      libraryDependencies ++= airframeDIDependencies,
      Compile / packageBin / mappings ++= (airspecCoreJVM / Compile / packageBin / mappings).value,
      Compile / packageSrc / mappings ++= (airspecCoreJVM / Compile / packageSrc / mappings).value
    )
    .jsSettings(
      airspecJSBuildSettings,
      Compile / packageBin / mappings ++= (airspecCoreJS / Compile / packageBin / mappings).value
        .filter(x => x._2 != "JS_DEPENDENCIES"),
      Compile / packageSrc / mappings ++= (airspecCoreJS / Compile / packageSrc / mappings).value
    )
    .dependsOn(airspecCore)

lazy val airspecDepsJVM = airspecDeps.jvm
lazy val airspecDepsJS  = airspecDeps.js

lazy val airspec =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("airspec"))
    .settings(buildSettings)
    .settings(
      airspecDependsOn := Seq("airspec"),
      airspecBuildSettings,
      name := "airspec",
      description := "AirSpec: A Functional Testing Framework for Scala",
      libraryDependencies ++= Seq(
        "org.scalacheck" %%% "scalacheck" % SCALACHECK_VERSION % Optional
      ),
      // A workaround for bloop, which cannot resolve Optional dependencies
      pomPostProcess := excludePomDependency(Seq("airspec-deps", "airspec_2.12", "airspec_2.13"))
    )
    .jvmSettings(
      // Embed dependent project codes to make airspec a single jar
      Compile / packageBin / mappings ++= (airspecDepsJVM / Compile / packageBin / mappings).value,
      Compile / packageSrc / mappings ++= (airspecDepsJVM / Compile / packageSrc / mappings).value,
      libraryDependencies ++= {
        scalaVersion.value match {
          case sv if sv.startsWith("3.") =>
            Seq(
              "org.scala-sbt" % "test-interface" % "1.0"
            )
          case sv =>
            Seq(
              "org.scala-lang" % "scala-reflect"  % sv,
              "org.scala-sbt"  % "test-interface" % "1.0"
            )
        }
      }
    )
    .jsSettings(
      Compile / packageBin / mappings ++= (airspecDepsJS / Compile / packageBin / mappings).value
        .filter(x => x._2 != "JS_DEPENDENCIES"),
      Compile / packageSrc / mappings ++= (airspecDepsJS / Compile / packageSrc / mappings).value,
      libraryDependencies ++= Seq(
        ("org.scala-js"        %% "scalajs-test-interface" % scalaJSVersion).cross(CrossVersion.for3Use2_13),
        ("org.portable-scala" %%% "portable-scala-reflect" % "1.1.1").cross(CrossVersion.for3Use2_13)
      )
    )
    .dependsOn(airspecDeps % Provided) // Use Provided dependency for bloop, and remove it later with pomPostProcess

lazy val airspecJVM = airspec.jvm
lazy val airspecJS  = airspec.js

def isAirSpecClass(mapping: (File, String)): Boolean =
  mapping._2.startsWith("wvlet/airspec/")

// An internal-only project for using AirSpec for testing Airframe modules
lazy val airspecRef =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("airspec-ref"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      //airspecBuildSettings,
      name := "airspec-ref",
      description := "A project for referencing airspec for internal testing",
      libraryDependencies += {
        "org.scalacheck" %%% "scalacheck" % SCALACHECK_VERSION
      }
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(airspec, airspecDeps)

lazy val airspecRefJVM = airspecRef.jvm
lazy val airspecRefJS  = airspecRef.js
// A JVM project containing only wvlet.airspec package classes
lazy val airspecLight =
  project
    .in(file("airspec-light"))
    .settings(buildSettings)
    .settings(
      name := "airspec-light",
      description := "API and and runner for AirSpec test cases",
      // Need to see the airspec source code directly to avoid any cyclic project references
      airspecDependsOn := Seq("airspec"),
      airspecBuildSettings,
      airspecJVMBuildSettings,
      // Extract only wvlet.airspec packages
      Compile / packageBin / mappings := (Compile / packageBin / mappings).value
        .filter(isAirSpecClass),
      Compile / packageSrc / mappings := (Compile / packageSrc / mappings).value
        .filter(isAirSpecClass),
      libraryDependencies ++= Seq(
        "org.scala-sbt"    % "test-interface" % "1.0"              % Provided,
        "org.scalacheck" %%% "scalacheck"     % SCALACHECK_VERSION % Provided
      )
    )
    .dependsOn(diJVM, metricsJVM)
