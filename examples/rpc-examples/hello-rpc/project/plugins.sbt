val SBT_AIRFRAME_VERSION = sys.env.getOrElse("SBT_AIRFRAME_VERSION", "2026.1.8")
addSbtPlugin("org.xerial.sbt"     % "sbt-pack"     % "1.0.0")
addSbtPlugin("org.wvlet.airframe" % "sbt-airframe" % SBT_AIRFRAME_VERSION)
addSbtPlugin("org.scalameta"      % "sbt-scalafmt" % "2.5.6")

conflictWarning := ConflictWarning.disable
