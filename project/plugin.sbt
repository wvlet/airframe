addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"             % "3.9.7")
addSbtPlugin("com.github.sbt"     % "sbt-pgp"                  % "2.1.2")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"            % "1.8.2")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"             % "2.4.2")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"            % "0.10.0")

addDependencyTreePlugin

// For Scala.js
val SCALAJS_VERSION = sys.env.getOrElse("SCALAJS_VERSION", "1.5.1")
addSbtPlugin("org.scala-js"  % "sbt-scalajs"         % SCALAJS_VERSION)
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0")
libraryDependencies ++= (
  Seq("org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0")
)

// For setting explicit versions for each commit
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

// Documentation
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.2.21")

// For generating Lexer/Parser from ANTLR4 grammar (.g4)
addSbtPlugin("com.simplytyped" % "sbt-antlr4" % "0.8.3")

// For JMH benchmark
addSbtPlugin("pl.project13.scala" % "sbt-jmh"  % "0.4.2")
addSbtPlugin("org.xerial.sbt"     % "sbt-pack" % "0.13")

scalacOptions ++= Seq("-deprecation", "-feature")

// Only for ScalaPB benchmark
//addSbtPlugin("com.thesamet"                    % "sbt-protoc"     % "0.99.34")
//libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.8"
