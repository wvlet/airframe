addSbtPlugin("com.github.gseitz" % "sbt-release"           % "1.0.6")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"          % "2.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"               % "1.1.0-M1")
addSbtPlugin("io.get-coursier"   % "sbt-coursier"          % "1.0.0-RC10")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"         % "1.5.0")
addSbtPlugin("com.lucidchart"    % "sbt-scalafmt-coursier" % "1.10")
addSbtPlugin("org.scala-js"      % "sbt-scalajs"           % "0.6.19")

addSbtPlugin("com.47deg" % "sbt-microsites" % "0.6.1")

scalacOptions ++= Seq("-deprecation", "-feature")
