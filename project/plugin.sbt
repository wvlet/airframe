resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.github.gseitz" % "sbt-release"   % "1.0.6")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"  % "2.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"       % "1.1.0")
addSbtPlugin("io.get-coursier"   % "sbt-coursier"  % "1.0.0-RC11")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"  % "1.3.0")
addSbtPlugin("org.scala-js"      % "sbt-scalajs"   % "0.6.20")

addSbtPlugin("com.47deg" % "sbt-microsites" % "0.7.1")

scalacOptions ++= Seq("-deprecation", "-feature")
