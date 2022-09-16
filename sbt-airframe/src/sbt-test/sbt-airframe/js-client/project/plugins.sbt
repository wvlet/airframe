ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("org.wvlet.airframe" % "sbt-airframe" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                               |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

// For Scala.js
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
val SCALAJS_VERSION = sys.env.getOrElse("SCALAJS_VERSION", "1.11.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % SCALAJS_VERSION)
