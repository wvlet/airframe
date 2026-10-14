val SBT_AIRFRAME_VERSION = sys.env.getOrElse("SBT_AIRFRAME_VERSION", "2026.2.3")
addSbtPlugin("org.wvlet.airframe" % "sbt-airframe"         % SBT_AIRFRAME_VERSION)
addSbtPlugin("org.scala-js"       % "sbt-scalajs"          % "1.22.0")
addSbtPlugin("org.wvlet.uni"      % "sbt-uni-crossproject" % "2026.1.25")
// Background fork-run (sbt-revolver replacement)
addSbtPlugin("org.wvlet.uni" % "sbt-uni" % "2026.1.25")

conflictWarning := ConflictWarning.disable
