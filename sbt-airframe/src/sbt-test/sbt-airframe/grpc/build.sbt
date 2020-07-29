import wvlet.airframe.sbt.http.AirframeHttpPlugin

val buildSettings: Seq[Def.Setting[_]] = Seq(
  testFrameworks += new TestFramework("wvlet.airspec.Framework"),
  libraryDependencies += "org.wvlet.airframe" %% "airspec" % "20.7.0" % "test"
)

lazy val root =
  project
    .settings(buildSettings)
    .aggregate(api, server)

lazy val api =
  project
    .in(file("api"))
    .settings(buildSettings)
    .settings(
      libraryDependencies += "org.wvlet.airframe" %% "airframe-http" % sys.props("plugin.version")
    )

lazy val server =
  project
    .in(file("server"))
    .enablePlugins(AirframeHttpPlugin)
    .settings(buildSettings)
    .settings(
      fork in Test := true,
      airframeHttpGeneratorOption := "-l trace",
      airframeHttpClients := Seq(
        "example.api:grpc-sync"
      ),
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-grpc" % sys.props("plugin.version")
      )
    )
    .dependsOn(api)
