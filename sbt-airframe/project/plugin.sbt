ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

addSbtPlugin("com.github.sbt" % "sbt-pgp"       % "2.3.2")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"  % "2.6.2")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo" % "0.14.0")

addDependencyTreePlugin

// For setting explicit versions for each commit
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

// Compile the root build's MinimizeJavadoc into this build, for the plugin's link-only javadoc page
Compile / unmanagedSources +=
  baseDirectory.value.getParentFile.getParentFile / "project" / "MinimizeJavadoc.scala"

// ScriptedPlugin is bundled with sbt 2.x itself, no separate scripted-plugin dependency needed

scalacOptions ++= Seq("-deprecation", "-feature")
