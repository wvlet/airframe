ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

addSbtPlugin("org.wvlet.airframe" % "sbt-airframe"             % "23.9.3")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.14.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("io.spray"           % "sbt-revolver"             % "0.10.0")
