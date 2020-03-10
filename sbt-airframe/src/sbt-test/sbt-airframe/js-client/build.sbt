lazy val root =
  crossProject.aggregate(spi, client)

lazy val spi =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("spi"))
    .settings(
      libraryDependencies += "org.wvlet.airframe" %%% "airframe-http" % sys.props("plugin.version")
    )

lazy val client =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("client"))
    .enablePlugins(AirframeHttpPlugin)
    .settings(
      airframeHttpPackages ++= Seq("myapp.spi")
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "1.0.0"
      )
    )
    .dependsOn(spi)
