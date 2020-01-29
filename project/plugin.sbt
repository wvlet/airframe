addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"             % "3.8.1")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"                  % "2.0.1")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"            % "1.6.1")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"             % "2.3.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")

// For Scala.js
val SCALAJS_VERSION = sys.env.getOrElse("SCALAJS_VERSION", "1.0.0-RC2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % SCALAJS_VERSION)
//addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.16.0")

libraryDependencies ++= (
  if (SCALAJS_VERSION.startsWith("1.0.0")) {
    // This plugin is available since Scala.js 1.0.0.
    // 1.0.0-RC3 has a hotfx for jsdom.createVritualConsole is not found error
    Seq("org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.0.0-RC3")
  } else {
    Seq.empty
  }
)

// For setting explicit versions for each commit
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")

// Documentation
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.1.1")

// For generating Lexer/Parser from ANTLR4 grammar (.g4)
addSbtPlugin("com.simplytyped" % "sbt-antlr4" % "0.8.2")

// For JMH benchmark
addSbtPlugin("pl.project13.scala" % "sbt-jmh"  % "0.3.7")
addSbtPlugin("org.xerial.sbt"     % "sbt-pack" % "0.12")

scalacOptions ++= Seq("-deprecation", "-feature")
