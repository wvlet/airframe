ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

ThisBuild / scalaVersion := "3.3.2"

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
      airframeHttpClients         := Seq("myapp.spi:rpc")
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "2.8.1"
      )
    )
    .dependsOn(spi)
