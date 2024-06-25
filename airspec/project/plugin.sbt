ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"             % "3.9.19")
addSbtPlugin("com.github.sbt"     % "sbt-pgp"                  % "2.2.1")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"            % "2.0.12")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"             % "2.4.5")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"            % "0.12.0")

addDependencyTreePlugin

// For Scala.js
val SCALAJS_VERSION = sys.env.getOrElse("SCALAJS_VERSION", "1.16.0")
addSbtPlugin("org.scala-js"  % "sbt-scalajs"         % SCALAJS_VERSION)
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")
libraryDependencies ++= (
  Seq("org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0")
)

// For Scala native
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.5.4")

// For setting explicit versions for each commit
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.0.1")

scalacOptions ++= Seq("-deprecation", "-feature", "-Xsource:3")
