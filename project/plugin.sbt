addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"             % "3.9.4")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"                  % "2.0.1")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"            % "1.6.1")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"             % "2.4.2")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"            % "0.10.0")

// For Scala.js
val SCALAJS_VERSION = sys.env.getOrElse("SCALAJS_VERSION", "1.1.1")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % SCALAJS_VERSION)

libraryDependencies ++= (
  Seq("org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0")
)

// For setting explicit versions for each commit
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

// Documentation
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.2.0")

// For generating Lexer/Parser from ANTLR4 grammar (.g4)
addSbtPlugin("com.simplytyped" % "sbt-antlr4" % "0.8.2")

// For JMH benchmark
addSbtPlugin("pl.project13.scala" % "sbt-jmh"  % "0.3.7")
addSbtPlugin("org.xerial.sbt"     % "sbt-pack" % "0.12")

// For sbt-airframe-http
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

scalacOptions ++= Seq("-deprecation", "-feature")
