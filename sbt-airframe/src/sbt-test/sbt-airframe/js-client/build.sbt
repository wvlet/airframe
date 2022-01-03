ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root =
  project.aggregate(spiJS, clientJS)

lazy val spi =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("spi"))
    .settings(
      libraryDependencies += "org.wvlet.airframe" %%% "airframe-http" % sys.props("airframe.version")
    )

lazy val spiJS = spi.js

lazy val client =
  crossProject(JSPlatform)
    .crossType(CrossType.Pure)
    .in(file("client"))
    .enablePlugins(AirframeHttpPlugin)
    .settings(
      airframeHttpGeneratorOption := "-l trace",
      airframeHttpClients         := Seq("myapp.spi:scalajs")
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "2.1.0"
      )
    )
    .dependsOn(spi)

lazy val clientJS = client.js
