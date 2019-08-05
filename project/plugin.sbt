addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"             % "2.3")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"                  % "2.0.0-M2")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"            % "1.5.1")
addSbtPlugin("com.geirsson"       % "sbt-scalafmt"             % "1.5.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.5.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "0.6.28")
addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"              % "0.6.2")
addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"               % "0.4.2")

// For setting explicit versions for each commit
addSbtPlugin("com.dwijnand" % "sbt-dynver"     % "3.0.0")
addSbtPlugin("com.47deg"    % "sbt-microsites" % "0.7.26")

// For generating Lexer/Parser from ANTLR4 grammar (.g4)
addSbtPlugin("com.simplytyped" % "sbt-antlr4" % "0.8.1")

// For JMH benchmark
addSbtPlugin("pl.project13.scala" % "sbt-jmh"  % "0.3.7")
addSbtPlugin("org.xerial.sbt"     % "sbt-pack" % "0.12")

scalacOptions ++= Seq("-deprecation", "-feature")
