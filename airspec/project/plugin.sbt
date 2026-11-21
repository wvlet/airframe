addSbtPlugin("com.github.sbt" % "sbt-pgp"       % "2.3.2")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"  % "2.6.2")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo" % "0.14.0")

addDependencyTreePlugin

// For Scala.js and Scala Native cross-building (sbt 2.x native)
addSbtPlugin("org.wvlet.uni" % "sbt-uni-crossproject" % "2026.1.41")

// For Scala.js
val SCALAJS_VERSION = sys.env.getOrElse("SCALAJS_VERSION", "1.23.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % SCALAJS_VERSION)

// For Scala native
val SCALA_NATIVE_VERSION = sys.env.getOrElse("SCALA_NATIVE_VERSION", "0.5.12")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % SCALA_NATIVE_VERSION)

// For setting explicit versions for each commit
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

// Strip unused Scaladoc assets (fonts, inkuire.js) from -javadoc.jar to stay within Maven Central
// publishing limits. See https://github.com/scala/scala3/issues/26387
addSbtPlugin("com.eed3si9n" % "sbt-salad-days" % "0.2.0")

// Compile the root build's javadoc settings into this build, so airspec follows the same policy
Compile / unmanagedSources ++= {
  val rootProject = baseDirectory.value.getParentFile.getParentFile / "project"
  Seq(rootProject / "MinimizeJavadoc.scala", rootProject / "MinimizeScaladoc.scala")
}

scalacOptions ++= Seq("-deprecation", "-feature")
