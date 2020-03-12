import wvlet.airframe.sbt.http.AirframeHttpPlugin

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
      airframeHttpClients := Seq(
        "myapp.spi",
        "myapp.spi:sync"
      ),
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-finagle" % sys.props("plugin.version")
      )
    )
    .dependsOn(spi)
