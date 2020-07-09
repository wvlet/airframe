enablePlugins(AirframeHttpPlugin)

airframeHttpOpenAPIPackages := Seq("example.api")
airframeHttpOpts := "-l debug"
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe-http" % sys.props("plugin.version")
)
