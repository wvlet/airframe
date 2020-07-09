import wvlet.airframe.sbt.http.AirframeHttpPlugin

project
  .enablePlugins(AirframeHttpPlugin)
  .settings(
    airframeHttpOpenAPIPackages := Seq(
      "example.api"
    ),
    libraryDependencies ++= Seq(
      "org.wvlet.airframe" %% "airframe-http" % sys.props("plugin.version")
    )
  )
