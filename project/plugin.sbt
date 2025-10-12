addSbtPlugin("com.github.sbt"      % "sbt-pgp"                  % "2.3.1")
addSbtPlugin("org.scalameta"       % "sbt-scalafmt"             % "2.5.5")
addSbtPlugin("org.portable-scala"  % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("com.eed3si9n"        % "sbt-buildinfo"            % "0.13.1")
addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings"         % "1.1.3")

// For auto-code rewrite
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.4")

// For integration testing
val SBT_AIRFRAME_VERSION = sys.env.getOrElse("SBT_AIRFRAME_VERSION", "24.12.2")
addSbtPlugin("org.wvlet.airframe" % "sbt-airframe" % SBT_AIRFRAME_VERSION)

addDependencyTreePlugin

// For Scala.js
val SCALAJS_VERSION = sys.env.getOrElse("SCALAJS_VERSION", "1.20.1")
addSbtPlugin("org.scala-js"  % "sbt-scalajs"         % SCALAJS_VERSION)
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")
libraryDependencies ++= (
  Seq("org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.1")
)

// For Scala.js + Playwright test
libraryDependencies += "io.github.gmkumar2005" %% "scala-js-env-playwright" % "0.1.18"

// For Scala native
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.5.9")

// For setting explicit versions for each commit
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

// Documentation
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.7.2")

// For generating Lexer/Parser from ANTLR4 grammar (.g4)
addSbtPlugin("com.simplytyped" % "sbt-antlr4" % "0.8.3")

// For JMH benchmark
addSbtPlugin("pl.project13.scala" % "sbt-jmh"  % "0.4.7")
addSbtPlugin("org.xerial.sbt"     % "sbt-pack" % "0.22")

scalacOptions ++= Seq("-deprecation", "-feature")

// Only for ScalaPB benchmark
//addSbtPlugin("com.thesamet"                    % "sbt-protoc"     % "0.99.34")
//libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.8"

// Binary compatibility checker
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.4")
