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
