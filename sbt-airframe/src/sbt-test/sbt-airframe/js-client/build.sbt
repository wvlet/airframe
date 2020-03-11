lazy val root =
  project.aggregate(spiJS, clientJS)

lazy val spi =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("spi"))
    .settings(
      libraryDependencies += "org.wvlet.airframe" %%% "airframe-http" % sys.props("plugin.version")
    )

lazy val spiJS = spi.js

lazy val client =
  crossProject(JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("client"))
    .enablePlugins(AirframeHttpPlugin)
    .settings(
      airframeHttpPackages ++= Seq("myapp.spi"),
      airframeHttpClientType := AirframeHttpPlugin.ScalaJSClient
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "1.0.0"
      )
    )
    .dependsOn(spi)

lazy val clientJS = client.js
