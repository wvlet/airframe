ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

addSbtPlugin("com.github.sbt" % "sbt-pgp"       % "2.3.1")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"  % "2.5.6")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo" % "0.13.1")

addDependencyTreePlugin

// For setting explicit versions for each commit
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

// For sbt-airframe
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

scalacOptions ++= Seq("-deprecation", "-feature")
