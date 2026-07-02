ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

addSbtPlugin("com.github.sbt" % "sbt-pgp"       % "2.3.1")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"  % "2.5.6")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo" % "0.13.1")

addDependencyTreePlugin

// For setting explicit versions for each commit
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

// ScriptedPlugin is bundled with sbt 2.x itself, no separate scripted-plugin dependency needed

scalacOptions ++= Seq("-deprecation", "-feature")
