ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val root =
  project.aggregate(spi.js, client.js)

lazy val spi =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("spi"))
    .settings(
      libraryDependencies += "org.wvlet.airframe" %%% "airframe-http" % sys.props("airframe.version")
    )

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

