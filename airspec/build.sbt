// A short cut for publishing snapshots to Sonatype
addCommandAlias(
  "publishSnapshots",
  s"+airspecJVM/publish; +airspecJS/publish"
)

// [Development purpose] publish all artifacts to the local repo
addCommandAlias(
  "publishAllLocal",
  s"+airspecJVM/publishLocal; +airspecJS/publishLocal;"
)

// Reload build.sbt on changes
Global / onChangedBuildSource := ReloadOnSourceChanges

val SCALA_2_12          = "2.12.17"
val SCALA_2_13          = "2.13.10"
val SCALA_3_0           = "3.2.1"
val targetScalaVersions = SCALA_3_0 :: SCALA_2_13 :: SCALA_2_12 :: Nil

val SCALACHECK_VERSION           = "1.17.0"
val JS_JAVA_LOGGING_VERSION      = "1.0.0"
val JAVAX_ANNOTATION_API_VERSION = "1.3.2"

ThisBuild / usePipelining := false

// We MUST use Scala 2.12 for building sbt-airframe
ThisBuild / scalaVersion := SCALA_2_13

ThisBuild / organization := "org.wvlet.airframe"

// Use dynamic snapshot version strings for non tagged versions
ThisBuild / dynverSonatypeSnapshots := true
// Use coursier friendly version separator
ThisBuild / dynverSeparator := "-"

// For Sonatype
// We need to define this globally as a workaround for https://github.com/sbt/sbt/pull/3760
ThisBuild / publishTo := sonatypePublishToBundle.value
sonatypeProfileName   := "org.wvlet"
sonatypeSessionName   := s"${sonatypeSessionName.value} for AirSpec"

// Share
ThisBuild / scalafmtConfig := file("../.scalafmt.conf")

val noPublish = Seq(
  publish / skip  := true,
  publishArtifact := false,
  publish         := {},
  publishLocal    := {},
  // Explicitly skip the doc task because protobuf related Java files causes no type found error
  Compile / doc / sources                := Seq.empty,
  Compile / packageDoc / publishArtifact := false
)

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
  pomPostProcess     := excludePomDependency(Seq("airspec_2.12", "airspec_2.13")),
  crossScalaVersions := targetScalaVersions,
  crossPaths         := true,
  publishMavenStyle  := true,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation"
  ) ++ {
    if (scalaVersion.value.startsWith("3.")) {
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
    if (scalaVersion.value.startsWith("3."))
      Seq.empty
    else
      Seq("org.scala-lang.modules" %%% "scala-collection-compat" % "2.8.1")
  }
)

def crossBuildSources(scalaBinaryVersion: String, baseDir: String, srcType: String = "main"): Seq[sbt.File] = {
  val scalaMajorVersion = scalaBinaryVersion.split("\\.").head
  for (suffix <- Seq("", s"-${scalaBinaryVersion}", s"-${scalaMajorVersion}").distinct)
    yield {
      file(s"${baseDir}/src/${srcType}/scala${suffix}")
    }
}

// https://stackoverflow.com/questions/41670018/how-to-prevent-sbt-to-include-test-dependencies-into-the-pom
import sbt.ThisBuild

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

/**
  * AirSpec build definitions.
  *
  * To make AirSpec a standalone library without any cyclic project references, AirSpec embeds the source code of
  * airframe-log, di, surface, etc.
  *
  * Since airframe-log, di, and surfaces uses Scala macros whose def-macros cannot be called within the same project, we
  * need to split the source code into 4 projects:
  *
  *   - airspec-log (dependsOn airframe-log's source)
  *   - airspec-core (di-macros, surface) # surface uses airframe-log macros
  *   - airspec-deps (di, metrics) # di uses di-macros
  *   - airspec (test-interface) # Need to split this as IntelliJ cannot find classes in unmanagedSourceDirectories
  *
  * airspec.jar will be an all-in-one jar with airframe-log, di, surface, metrics, etc.
  */
val airspecLogDependencies  = Seq("airframe-log")
val airspecCoreDependencies = Seq("airframe-di-macros", "airframe-surface")
val airspecDependencies     = Seq("airframe-di", "airframe-metrics")

// Setting keys for AirSpec
val airspecDependsOn =
  settingKey[Seq[String]]("Dependent module names of airspec projects")

// Read sources from the sibling projects
val airspecBuildSettings = Seq[Setting[_]](
  Compile / unmanagedSourceDirectories ++= {
    val baseDir = (ThisBuild / baseDirectory).value.getAbsoluteFile
    val sv      = scalaBinaryVersion.value
    val sourceDirs =
      for (m <- airspecDependsOn.value; infix <- Seq("")) yield {
        crossBuildSources(sv, s"${baseDir}/../${m}${infix}")
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
        crossBuildSources(sv, s"${baseDir}/../${m}/${folder}")
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
        crossBuildSources(sv, s"${baseDir}/../${m}/${folder}")
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
      name        := "airspec-log",
      description := "airframe-log for AirSpec",
      libraryDependencies ++= {
        scalaVersion.value match {
          case s if s.startsWith("3.") =>
            Seq.empty
          case v =>
            Seq("org.scala-lang" % "scala-reflect" % v % Provided)
        }
      }
    )
    .jvmSettings(
      airspecJVMBuildSettings,
      libraryDependencies ++= Seq(
        // For rotating log files
        "ch.qos.logback" % "logback-core" % "1.3.4"
      )
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
      name        := "airspec-core",
      description := "A core module of AirSpec with Surface and DI macros",
      libraryDependencies ++= {
        scalaVersion.value match {
          case s if s.startsWith("3.") =>
            Seq.empty
          case v =>
            Seq(
              ("org.scala-lang" % "scala-reflect"  % v),
              ("org.scala-lang" % "scala-compiler" % v % Provided)
            )
        }
      }
    )
    .jvmSettings(
      airspecJVMBuildSettings,
      libraryDependencies ++= {
        scalaVersion.value match {
          case s if s.startsWith("3.") =>
            Seq(
              "org.scala-lang" %% "scala3-tasty-inspector" % s,
              "org.scala-lang" %% "scala3-staging"         % s
            )
          case _ => Seq.empty
        }
      },
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
      name        := "airspec-deps",
      description := "Dependencies of AirSpec"
    )
    .jvmSettings(
      airspecJVMBuildSettings,
      libraryDependencies ++= Seq(
        "javax.annotation" % "javax.annotation-api" % JAVAX_ANNOTATION_API_VERSION
      ),
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
    .in(file("."))
    .settings(buildSettings)
    .settings(
      airspecDependsOn := Seq.empty,
      airspecBuildSettings,
      name        := "airspec",
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
        ("org.portable-scala" %%% "portable-scala-reflect" % "1.1.2").cross(CrossVersion.for3Use2_13),
        // Necessary for async testing
        "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.0"
      )
    )
    // This should be Optional dependency, but using Provided dependency for bloop which doesn't support Optional.
    // This provided dependency will be removed later with pomPostProcess
    .dependsOn(airspecDeps % Provided)

lazy val airspecJVM = airspec.jvm
lazy val airspecJS  = airspec.js
