ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

addSbtPlugin("com.github.sbt"     % "sbt-pgp"                  % "2.3.1")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"            % "2.2.2")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"             % "2.4.5")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"            % "0.13.1")

addDependencyTreePlugin

// For Scala.js
val SCALAJS_VERSION          = sys.env.getOrElse("SCALAJS_VERSION", "1.19.0")
addSbtPlugin("org.scala-js"  % "sbt-scalajs"         % SCALAJS_VERSION)
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")
libraryDependencies ++= (
  Seq("org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0")
)

// For Scala native
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.5.7")

// For setting explicit versions for each commit
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.0")

scalacOptions ++= Seq("-deprecation", "-feature", "-Xsource:3")
