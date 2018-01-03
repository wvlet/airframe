resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.github.gseitz"  % "sbt-release"              % "1.0.7")
addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"             % "2.0")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"                  % "1.1.0")
addSbtPlugin("io.get-coursier"    % "sbt-coursier"             % "1.0.0")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"            % "1.5.1")
addSbtPlugin("com.geirsson"       % "sbt-scalafmt"             % "1.4.0")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "0.6.21")
addSbtPlugin("org.portable-scala" % "sbt-crossproject"         % "0.3.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.3.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"              % "0.6.2")
// For setting explicit versions for each commit
addSbtPlugin("com.dwijnand"       % "sbt-dynver"               % "2.0.0")
addSbtPlugin("com.47deg"          % "sbt-microsites"           % "0.7.13")

scalacOptions ++= Seq("-deprecation", "-feature")
