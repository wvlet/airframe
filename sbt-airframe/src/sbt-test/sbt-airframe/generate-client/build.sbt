lazy val root =
  project.aggregate(spi, server)

lazy val spi =
  project
    .in(file("spi"))
    .settings(
      libraryDependencies += "org.wvlet.airframe" %% "airframe-http" % sys.props("plugin.version")
    )

lazy val server =
  project
    .in(file("server"))
    .enablePlugins(AirframeHttpPlugin)
    .settings(
      airframeHttpPackages ++= Seq("myapp.spi")
    )
    .dependsOn(spi)
