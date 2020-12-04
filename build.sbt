import sbtcrossproject.{CrossType, crossProject}
import xerial.sbt.pack.PackPlugin.publishPackArchiveTgz

val SCALA_2_12          = "2.12.12"
val SCALA_2_13          = "2.13.4"
val SCALA_3_0           = "3.0.0-M2"
val targetScalaVersions = SCALA_2_13 :: SCALA_2_12 :: Nil
val withDotty           = SCALA_3_0 :: targetScalaVersions

val SCALATEST_VERSION               = "3.0.8"
val SCALACHECK_VERSION              = "1.15.1"
val MSGPACK_VERSION                 = "0.8.21"
val SCALA_PARSER_COMBINATOR_VERSION = "1.1.2"
val SQLITE_JDBC_VERSION             = "3.32.3.2"
val SLF4J_VERSION                   = "1.7.30"
val JS_JAVA_LOGGING_VERSION         = "1.0.0"
val JS_JAVA_TIME_VERSION            = "1.0.0"
val SCALAJS_DOM_VERSION             = "1.1.0"
val FINAGLE_VERSION                 = "20.10.0"
val FLUENCY_VERSION                 = "2.5.0"
val GRPC_VERSION                    = "1.34.0"
val JMH_VERSION                     = "1.26"
val AIRSPEC_VERSION                 = "20.12.0"

val airSpecFramework = new TestFramework("wvlet.airspec.Framework")

// Publish only Scala 2.12 projects for snapshot releases
addCommandAlias(
  "publishSnapshots",
  s"; ++ ${SCALA_2_12}; projectJVM/publish; projectJS/publish; sbtAirframe/publish;"
)
addCommandAlias(
  "publishJVMLocal",
  s"; ++ ${SCALA_2_12}; projectJVM/publishLocal; sbtAirframe/publishLocal;"
)

addCommandAlias(
  "publishJSSigned",
  s"; + projectJS/publishSigned;"
)
addCommandAlias(
  "publishJSLocal",
  s"; + projectJS/publishLocal; "
)

// Allow using Ctrl+C in sbt without exiting the prompt
// cancelable in Global := true

//ThisBuild / turbo := true

// Reload build.sbt on changes
Global / onChangedBuildSource := ReloadOnSourceChanges

// Disable the pipelining available since sbt-1.4.0. It caused compilation failure
ThisBuild / usePipelining := false

// For using Scala 2.12 in sbt
val IS_DOTTY = sys.env.isDefinedAt("DOTTY")
scalaVersion in ThisBuild := { if (IS_DOTTY) SCALA_3_0 else SCALA_2_12 }
organization in ThisBuild := "org.wvlet.airframe"

// Use dynamic snapshot version strings for non tagged versions
dynverSonatypeSnapshots in ThisBuild := true
// Use coursier friendly version separator
dynverSeparator in ThisBuild := "-"

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
  // which cannot resolve Optional dependencies nor compile-internal dependencie.
  pomPostProcess := excludePomDependency(Seq("airframe-di-macros")),
  crossScalaVersions := targetScalaVersions,
  crossPaths := true,
  publishMavenStyle := true,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    // Necessary for tracking source code range in airframe-rx demo
    "-Yrangepos"
  ) ++ (if (IS_DOTTY) Seq("-Ytasty-reader") else Seq.empty),
  testFrameworks += airSpecFramework,
  libraryDependencies ++= {
    if (IS_DOTTY)
      Seq.empty
    else
      Seq("org.scala-lang.modules" %%% "scala-collection-compat" % "2.3.1")
  }
)

// Do not run tests concurrently to avoid JMX registration failures
val runTestSequentially = Seq[Setting[_]](parallelExecution in Test := false)

// We need to define this globally as a workaround for https://github.com/sbt/sbt/pull/3760
publishTo in ThisBuild := sonatypePublishToBundle.value

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
    .aggregate((jvmProjects ++ jsProjects ++ sbtProjects): _*)

// JVM projects for scala-community build. This should have no tricky setup and should support Scala 2.12.
lazy val communityBuildProjects: Seq[ProjectReference] = Seq(
  airframeJVM,
  surfaceJVM,
  logJVM,
  airframeScalaTest,
  canvas,
  config,
  controlJVM,
  jmx,
  launcher,
  metricsJVM,
  codecJVM,
  msgpackJVM,
  rxJVM,
  httpJVM,
  grpc,
  jsonJVM,
  rxHtmlJVM,
  airspecJVM
)

// Other JVM projects supporting Scala 2.12 - Scala 2.13
lazy val jvmProjects: Seq[ProjectReference] = communityBuildProjects ++ Seq[ProjectReference](
  jdbc,
  fluentd,
  airspecLight,
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
  airframeJS,
  metricsJS,
  airspecJS,
  controlJS,
  jsonJS,
  msgpackJS,
  codecJS,
  rxJS,
  httpJS,
  rxHtmlJS,
  widgetJS
)

lazy val sbtProjects: Seq[ProjectReference] = Seq(sbtAirframe)

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

lazy val docs =
  project
    .in(file("airframe-docs"))
    .settings(
      name := "airframe-docs",
      moduleName := "airframe-docs",
      publishArtifact := false,
      publish := {},
      publishLocal := {},
      watchTriggers in mdoc += ((ThisBuild / baseDirectory).value / "docs").toGlob / ** / "*.md"
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
  def isExcludeTarget(artifactId: String): Boolean = excludes.exists(artifactId.startsWith(_))
  def artifactId(e: Elem): Option[String]          = e.child.find(_.label == "artifactId").map(_.text.trim())
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
      // testing shutdown hooks requires consistent application lifecycle between sbt and JVM https://github.com/sbt/sbt/issues/4794
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
    .dependsOn(
      surface,
      // Include airframe-di-macros as provided (for bloop) and remove it from pom.xml
      airframeMacros % Provided,
      airspecRef     % Test
    )

lazy val airframeJVM = airframe.jvm
lazy val airframeJS  = airframe.js

// Airframe DI needs to call macro methods, so we needed to split the project into DI and DI macros.
// This project sources and classes will be embedded to airframe.jar, so we don't publish airframe-di-macros
lazy val airframeMacros =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-di-macros"))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(
      name := "airframe-di-macros",
      description := "Macros for Airframe",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      ),
      // As a workaround for build-pipelining failure at sbt 1.4.0-RC2
      exportPipelining := false
    )
    .jsSettings(jsBuildSettings)

lazy val airframeMacrosJVM = airframeMacros.jvm
lazy val airframeMacrosJS  = airframeMacros.js

// To use airframe in other airframe modules, we need to reference airframeMacros project
lazy val airframeMacrosJVMRef = airframeMacrosJVM % Optional
lazy val airframeMacrosRef    = airframeMacros    % Optional

val surfaceDependencies = { scalaVersion: String =>
  Seq(
    // For ading PreDestroy, PostConstruct annotations to Java9
    "javax.annotation" % "javax.annotation-api" % "1.3.2",
    "org.scala-lang"   % "scala-reflect"        % scalaVersion,
    "org.scala-lang"   % "scala-compiler"       % scalaVersion % Provided
  )
}

lazy val surface =
  crossProject(JVMPlatform, JSPlatform)
    .in(file("airframe-surface"))
    .settings(buildSettings)
    .settings(
      name := "airframe-surface",
      description := "A library for extracting object structure surface",
      libraryDependencies ++= surfaceDependencies(scalaVersion.value)
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
        "org.yaml" % "snakeyaml" % "1.27"
      )
    )
    .dependsOn(airframeJVM, airframeMacrosJVMRef, codecJVM, airspecRefJVM % Test)

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
    case s if IS_DOTTY =>
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
    .in(file("airframe-log"))
    .settings(buildSettings)
    .settings(
      name := "airframe-log",
      description := "Fancy logger for Scala",
      libraryDependencies ++= logDependencies(scalaVersion.value)
    )
    .jvmSettings(
      libraryDependencies ++= logJVMDependencies,
      runTestSequentially
    )
    .jsSettings(
      jsBuildSettings,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-java-logging" % JS_JAVA_LOGGING_VERSION
      )
    )
    .dependsOn(airspecRef % Test)

lazy val logJVM = log.jvm
lazy val logJS  = log.js

lazy val metrics =
  crossProject(JVMPlatform, JSPlatform)
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

lazy val airframeScalaTest =
  project
    .in(file("airframe-scalatest"))
    .settings(buildSettings)
    .settings(
      name := "airframe-scalatest",
      description := "A handy base trait for writing test using ScalaTest",
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % SCALATEST_VERSION
      )
    )
    .dependsOn(logJVM)

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
      libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % JS_JAVA_TIME_VERSION
    )
    .dependsOn(log, json, airspecRef % Test)

lazy val msgpackJVM = msgpack.jvm
lazy val msgpackJS  = msgpack.js

lazy val codec =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-codec"))
    .settings(buildSettings)
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
    .dependsOn(log, surface, msgpack, metrics, json, control, airspecRef % Test)

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
        "org.postgresql" % "postgresql"  % "42.2.18",
        "com.zaxxer"     % "HikariCP"    % "3.4.5",
        // For routing slf4j log to airframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(airframeJVM, airframeMacrosJVMRef, controlJVM, config, airspecRefJVM % Test)

lazy val rx =
  crossProject(JVMPlatform, JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-rx"))
    .settings(buildSettings)
    .settings(
      name := "airframe-rx",
      description := "Reactive stream (Rx) interface"
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
    .in(file("airframe-http"))
    .settings(buildSettings)
    .settings(
      name := "airframe-http",
      description := "REST API Framework"
    )
    .jsSettings(
      jsBuildSettings,
      jsEnv in Test := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % SCALAJS_DOM_VERSION
      )
    )
    .dependsOn(airframe, airframeMacrosRef, rx, control, surface, json, codec, airspecRef % Test)

lazy val httpJVM = http.jvm
  .enablePlugins(PackPlugin)
  .settings(
    packMain := Map("airframe-http-code-generator" -> "wvlet.airframe.http.codegen.HttpCodeGenerator"),
    packExcludeLibJars := Seq("airspec_2.12"),
    publishPackArchiveTgz,
    libraryDependencies ++= Seq(
      // Use swagger-parser only for validating YAML format in tests
      "io.swagger.parser.v3" % "swagger-parser" % "2.0.23" % Test,
      // Swagger includes dependency to SLF4J, so redirect slf4j logs to airframe-log
      "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION % Test
    )
  ).dependsOn(launcher)

lazy val httpJS = http.js

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
    ).dependsOn(httpJVM, rxJVM, airframeMacrosJVMRef, airspecRefJVM % Test)

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
    .dependsOn(httpJVM, airframeMacrosJVMRef, airspecRefJVM % Test)

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
    .dependsOn(httpJVM, airframeMacrosJVMRef, finagle % Test, airspecRefJVM % Test)

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
    .dependsOn(codecJVM, metricsJVM, controlJVM, finagle, jdbc, airframeMacrosJVMRef, airspecRefJVM % Test)

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
      pack := pack.dependsOn(compile in Test).value,
      sourceDirectory in Jmh := (sourceDirectory in Compile).value,
      compile in Jmh := (compile in Jmh).triggeredBy(compile in Compile).value,
      compile in Test := ((compile in Test).dependsOn(compile in Jmh)).value,
      // Need to fork JVM so that sbt can set the classpass properly for running JMH
      fork in run := true,
      libraryDependencies ++= Seq(
        "org.msgpack"     % "msgpack-core"             % MSGPACK_VERSION,
        "org.openjdk.jmh" % "jmh-core"                 % JMH_VERSION,
        "org.openjdk.jmh" % "jmh-generator-bytecode"   % JMH_VERSION,
        "org.openjdk.jmh" % "jmh-generator-reflection" % JMH_VERSION,
        // Used only for json benchmark
        "org.json4s" %% "json4s-jackson" % "3.6.10",
        "io.circe"   %% "circe-parser"   % "0.13.0",
        // For ScalaPB
        // "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
        // For grpc-java
        "io.grpc"             % "grpc-protobuf" % GRPC_VERSION,
        "com.google.protobuf" % "protobuf-java" % "3.14.0"
      )
//      PB.targets in Compile := Seq(
//        scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
//      ),
      // publishing .tgz
      // publishPackArchiveTgz
    )
    .dependsOn(msgpackJVM, jsonJVM, metricsJVM, launcher, finagle, grpc, airframeMacrosJVMRef, airspecRefJVM % Test)

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
        "org.komamitsu" % "fluency-treasuredata" % FLUENCY_VERSION % Provided,
        // Redirecting slf4j log from Fluency to aiframe-log
        "org.slf4j" % "slf4j-jdk14" % SLF4J_VERSION
      )
    )
    .dependsOn(codecJVM, airframeJVM, airframeMacrosJVMRef, airspecRefJVM % Test)

def sqlRefLib = { scalaVersion: String =>
  if (scalaVersion.startsWith("2.12")) {
    Seq(
      // Include Spark just as a reference implementation
      "org.apache.spark" %% "spark-sql" % "3.0.1" % Test,
      // Include Presto as a reference implementation
      "io.prestosql" % "presto-main" % "347" % Test
    )
  } else {
    Seq.empty
  }
}

lazy val sql =
  project
    .enablePlugins(Antlr4Plugin)
    .in(file("airframe-sql"))
    .settings(buildSettings)
    .settings(
      name := "airframe-sql",
      description := "SQL parser & analyzer",
      antlr4Version in Antlr4 := "4.9",
      antlr4PackageName in Antlr4 := Some("wvlet.airframe.sql.parser"),
      antlr4GenListener in Antlr4 := true,
      antlr4GenVisitor in Antlr4 := true,
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
      libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    )
    .jsSettings(
      jsBuildSettings,
      jsEnv in Test := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % SCALAJS_DOM_VERSION
      )
    )
    .dependsOn(log, rx, surface, airspecRef % Test)

lazy val rxHtmlJVM = rxHtml.jvm
lazy val rxHtmlJS  = rxHtml.js

lazy val widget =
  crossProject(JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("airframe-rx-widget"))
    //    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(buildSettings)
    .settings(
      name := "airframe-rx-widget",
      description := "Reactive Widget library for Scala.js"
    )
    .jsSettings(
      jsBuildSettings,
      jsEnv in Test := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
//      npmDependencies in Test += "node" -> "12.14.1"
    )
    .dependsOn(log, rxHtml, airspecRef % Test)

lazy val widgetJS = widget.js

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
      airframeJVM,
      airframeMacrosJVM,
      launcher,
      jmx,
      jdbc,
      finagle,
      airspecRefJVM % Test
    )

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
val airspecDependencies     = Seq("airframe", "airframe-metrics")

// Setting keys for AirSpec
val airspecDependsOn = settingKey[Seq[String]]("Dependent module names of airspec projects")

val airspecBuildSettings = Seq[Setting[_]](
  unmanagedSourceDirectories in Compile ++= {
    val baseDir = (ThisBuild / baseDirectory).value.getAbsoluteFile
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
    val baseDir = (ThisBuild / baseDirectory).value.getAbsoluteFile
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
    val baseDir = (ThisBuild / baseDirectory).value.getAbsoluteFile
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
      mappings in (Compile, packageBin) ++= mappings.in(airspecLogJVM, Compile, packageBin).value,
      mappings in (Compile, packageSrc) ++= mappings.in(airspecLogJVM, Compile, packageSrc).value
    )
    .jsSettings(
      airspecJSBuildSettings,
      mappings in (Compile, packageBin) ++= mappings
        .in(airspecLogJS, Compile, packageBin).value.filter(x => x._2 != "JS_DEPENDENCIES"),
      mappings in (Compile, packageSrc) ++= mappings.in(airspecLogJS, Compile, packageSrc).value
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
      mappings in (Compile, packageBin) ++= mappings.in(airspecCoreJVM, Compile, packageBin).value,
      mappings in (Compile, packageSrc) ++= mappings.in(airspecCoreJVM, Compile, packageSrc).value
    )
    .jsSettings(
      airspecJSBuildSettings,
      mappings in (Compile, packageBin) ++= mappings
        .in(airspecCoreJS, Compile, packageBin).value.filter(x => x._2 != "JS_DEPENDENCIES"),
      mappings in (Compile, packageSrc) ++= mappings.in(airspecCoreJS, Compile, packageSrc).value
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
      name := "airspec",
      description := "AirSpec: A Functional Testing Framework for Scala",
      libraryDependencies ++= Seq(
        "org.scalacheck" %%% "scalacheck" % SCALACHECK_VERSION % Optional
      ),
      // A workaround for bloop, which cannot resolve Optional dependencies
      pomPostProcess := excludePomDependency(Seq("airspec-deps"))
    )
    .jvmSettings(
      // Embed dependent project codes to make airspec a single jar
      mappings in (Compile, packageBin) ++= mappings.in(airspecDepsJVM, Compile, packageBin).value,
      mappings in (Compile, packageSrc) ++= mappings.in(airspecDepsJVM, Compile, packageSrc).value,
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
        "org.scala-sbt"  % "test-interface" % "1.0"
      )
    )
    .jsSettings(
      mappings in (Compile, packageBin) ++= mappings
        .in(airspecDepsJS, Compile, packageBin).value.filter(x => x._2 != "JS_DEPENDENCIES"),
      mappings in (Compile, packageSrc) ++= mappings.in(airspecDepsJS, Compile, packageSrc).value,
      libraryDependencies ++= Seq(
        "org.scala-js"        %% "scalajs-test-interface" % scalaJSVersion,
        "org.portable-scala" %%% "portable-scala-reflect" % "1.0.0"
      )
    )
    .dependsOn(airspecDeps % Provided) // Use Provided dependency for bloop, and remove it later with pomPostProcess

lazy val airspecJVM = airspec.jvm
lazy val airspecJS  = airspec.js

def isAirSpecClass(mapping: (File, String)): Boolean = mapping._2.startsWith("wvlet/airspec/")

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
      mappings in (Compile, packageBin) := mappings.in(Compile, packageBin).value.filter(isAirSpecClass),
      mappings in (Compile, packageSrc) := mappings.in(Compile, packageSrc).value.filter(isAirSpecClass),
      libraryDependencies ++= Seq(
        "org.scala-sbt"    % "test-interface" % "1.0"              % Provided,
        "org.scalacheck" %%% "scalacheck"     % SCALACHECK_VERSION % Provided
      )
    )
    .dependsOn(airframeJVM, airframeMacrosJVMRef, metricsJVM)

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
      libraryDependencies ++= {
        Seq("org.scalacheck" %%% "scalacheck" % SCALACHECK_VERSION)
      }
    )
    .jsSettings(jsBuildSettings)
    .dependsOn(airspec, airspecDeps)

lazy val airspecRefJVM = airspecRef.jvm
lazy val airspecRefJS  = airspecRef.js

// sbt plugin

lazy val sbtAirframe =
  project
    .in(file("sbt-airframe"))
    .enablePlugins(SbtPlugin, BuildInfoPlugin)
    .settings(
      buildSettings,
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "wvlet.airframe.sbt",
      name := "sbt-airframe",
      description := "sbt plugin for helping programming with Airframe",
      scalaVersion := SCALA_2_12,
      // This setting might be unnecessary?
      //crossSbtVersions := Vector("1.3.13"),
      libraryDependencies ++= Seq(
        "io.get-coursier"   %% "coursier"         % "2.0.7",
        "org.apache.commons" % "commons-compress" % "1.20"
      ),
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
      },
      scriptedDependencies := {
        // Publish all dependencies necessary for running the scripted tests
        val depPublish = scriptedDependencies.value
        val p1         = publishLocal.in(httpJVM, packArchiveTgz).value
        val p2         = publishLocal.all(ScopeFilter(inDependencies(finagle))).value
        val p3         = publishLocal.all(ScopeFilter(inDependencies(grpc))).value
        val p4         = publishLocal.all(ScopeFilter(inDependencies(airspecJVM))).value
        val p5         = publishLocal.all(ScopeFilter(inDependencies(httpJS))).value
      },
      scriptedBufferLog := false
    )
    .dependsOn(controlJVM, codecJVM, logJVM, httpJVM % Test, airspecRefJVM % Test)
