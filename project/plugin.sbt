addSbtPlugin("com.github.sbt"      % "sbt-pgp"          % "2.3.1")
addSbtPlugin("org.scalameta"       % "sbt-scalafmt"     % "2.5.6")
addSbtPlugin("com.eed3si9n"        % "sbt-buildinfo"    % "0.13.1")
addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.4")

// For auto-code rewrite
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.5")

addDependencyTreePlugin

// For Scala.js and Scala Native cross-building (sbt 2.x native)
addSbtPlugin("org.wvlet.uni" % "sbt-uni-crossproject" % "2026.1.14")

// For background fork-run (sbt-revolver replacement) and HTTP/RPC client code generation
addSbtPlugin("org.wvlet.uni" % "sbt-uni" % "2026.1.14")

// For Scala.js DOM tests. Replaces scalajs-env-jsdom-nodejs, which has no Scala 3 build and is
// no longer binary-compatible with current Scala.js jsenv APIs. Provides PlaywrightJSEnv.
addSbtPlugin("org.wvlet.uni" % "sbt-uni-playwright" % "2026.1.14")

// For Scala.js
val SCALAJS_VERSION = sys.env.getOrElse("SCALAJS_VERSION", "1.22.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % SCALAJS_VERSION)

// For Scala native
val SCALA_NATIVE_VERSION = sys.env.getOrElse("SCALA_NATIVE_VERSION", "0.5.12")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % SCALA_NATIVE_VERSION)

// For setting explicit versions for each commit
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

// Documentation
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.8.2")

// For JMH benchmark
addSbtPlugin("pl.project13.scala" % "sbt-jmh"  % "0.4.8")
addSbtPlugin("org.xerial.sbt"     % "sbt-pack" % "1.0.0")

// For generating Lexer/Parser from ANTLR4 grammar (.g4) via a custom sourceGenerators task,
// as sbt-antlr4 has no sbt 2.x build
libraryDependencies += "org.antlr" % "antlr4" % "4.13.2"

// For integration testing.
// Defaults to the next sbt-airframe release. Until that is published to Maven Central,
// build sbt-airframe locally and point this at the snapshot:
//   (cd sbt-airframe && ../sbt publishLocal)
//   export SBT_AIRFRAME_VERSION=$(./scripts/dynver.sh)
val SBT_AIRFRAME_VERSION = sys.env.getOrElse("SBT_AIRFRAME_VERSION", "2026.2.0")
addSbtPlugin("org.wvlet.airframe" % "sbt-airframe" % SBT_AIRFRAME_VERSION)

scalacOptions ++= Seq("-deprecation", "-feature")

// Only for ScalaPB benchmark
//addSbtPlugin("com.thesamet"                    % "sbt-protoc"     % "0.99.34")
//libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.8"

// Binary compatibility checker
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.6")

conflictWarning := ConflictWarning.disable
