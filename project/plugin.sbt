resolvers += Resolver.sonatypeRepo("snapshots")

val scalaJSVersion = Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.21")

addSbtPlugin("com.github.gseitz" % "sbt-release"              % "1.0.6")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"             % "2.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"                  % "1.1.0")
addSbtPlugin("io.get-coursier"   % "sbt-coursier"             % "1.0.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"            % "1.5.1")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"             % "1.3.0")
addSbtPlugin("org.scala-js"      % "sbt-scalajs"              % scalaJSVersion)
addSbtPlugin("org.portable-scala"  % "sbt-crossproject"         % "0.3.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.3.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-ghpages"              % "0.6.2")

addSbtPlugin("com.47deg" % "sbt-microsites" % "0.7.1")

// For Node.js with jsdom
libraryDependencies ++= {
  if (scalaJSVersion.startsWith("0.6.")) Nil
  else Seq("org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.0.0-M2")
}

scalacOptions ++= Seq("-deprecation", "-feature")
