addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"             % "3.8.1")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"                  % "2.0.1")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"            % "1.6.1")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"             % "2.3.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")

val SCALA_JS_VERSION = sys.env.getOrElse("SCALA_JS_VERSION", "1.0.0-RC2")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % SCALA_JS_VERSION)
libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % SCALA_JS_VERSION

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
