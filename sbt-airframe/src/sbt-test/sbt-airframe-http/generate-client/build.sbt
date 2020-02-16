libraryDependencies += "org.wvlet.airframe" %% "airframe-http" % "20.2.0"

enablePlugins(AirframePlugin)

airframeHttpPackages ++= Seq("example")
