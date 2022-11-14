ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype"  % "3.9.14")
addSbtPlugin("com.github.sbt" % "sbt-pgp"       % "2.2.0")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"  % "2.5.0")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo" % "0.11.0")

addDependencyTreePlugin

// For setting explicit versions for each commit
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

// For sbt-airframe
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

scalacOptions ++= Seq("-deprecation", "-feature")
