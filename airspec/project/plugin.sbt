addSbtPlugin("com.github.sbt" % "sbt-pgp"       % "2.3.1")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"  % "2.5.6")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo" % "0.13.1")

addDependencyTreePlugin

// For Scala.js and Scala Native cross-building (sbt 2.x native)
addSbtPlugin("org.wvlet.uni" % "sbt-uni-crossproject" % "2026.1.14")

// For Scala.js
val SCALAJS_VERSION = sys.env.getOrElse("SCALAJS_VERSION", "1.22.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % SCALAJS_VERSION)

// For Scala native
val SCALA_NATIVE_VERSION = sys.env.getOrElse("SCALA_NATIVE_VERSION", "0.5.12")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % SCALA_NATIVE_VERSION)

// For setting explicit versions for each commit
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

scalacOptions ++= Seq("-deprecation", "-feature")
