addSbtPlugin("org.xerial.sbt"   % "sbt-sonatype"  % "2.3")
addSbtPlugin("com.jsuereth"     % "sbt-pgp"       % "1.1.1")
addSbtPlugin("io.get-coursier"  % "sbt-coursier"  % "1.1.0-M7")
addSbtPlugin("org.scoverage"    % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.geirsson"     % "sbt-scalafmt"  % "1.5.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages"   % "0.6.2")
addSbtPlugin("com.eed3si9n"     % "sbt-unidoc"    % "0.4.2")

addSbtPlugin("com.simplytyped" % "sbt-antlr4" % "0.8.1")

// For setting explicit versions for each commit
addSbtPlugin("com.dwijnand" % "sbt-dynver"     % "3.0.0")
addSbtPlugin("com.47deg"    % "sbt-microsites" % "0.7.26")

scalacOptions ++= Seq("-deprecation", "-feature")
