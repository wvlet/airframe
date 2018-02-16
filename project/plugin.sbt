resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"             % "2.1")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"                  % "1.1.0")
addSbtPlugin("io.get-coursier"    % "sbt-coursier"             % "1.0.2")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"            % "1.5.1")
addSbtPlugin("com.geirsson"       % "sbt-scalafmt"             % "1.4.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "0.6.22")
addSbtPlugin("org.portable-scala" % "sbt-crossproject"         % "0.3.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.3.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"              % "0.6.2")
addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"               % "0.4.1")
// For setting explicit versions for each commit
addSbtPlugin("com.dwijnand" % "sbt-dynver"     % "2.0.0")
addSbtPlugin("com.47deg"    % "sbt-microsites" % "0.7.15")

scalacOptions ++= Seq("-deprecation", "-feature")
