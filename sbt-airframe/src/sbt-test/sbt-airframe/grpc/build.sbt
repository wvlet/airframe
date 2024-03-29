import wvlet.airframe.sbt.http.AirframeHttpPlugin

val AIRSPEC_VERSION = sys.props("airspec.version")

val buildSettings: Seq[Def.Setting[?]] = Seq(
  testFrameworks += new TestFramework("wvlet.airspec.Framework"),
  libraryDependencies += "org.wvlet.airframe" %% "airspec" % AIRSPEC_VERSION % Test
)
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")
ThisBuild / scalaVersion := "3.3.2"

lazy val root =
  project
    .settings(buildSettings)
    .aggregate(api, server)

lazy val api =
  project
    .in(file("api"))
    .settings(buildSettings)
    .settings(
      libraryDependencies += "org.wvlet.airframe" %% "airframe-http" % sys.props("airframe.version")
    )

lazy val server =
  project
    .in(file("server"))
    .enablePlugins(AirframeHttpPlugin)
    .settings(buildSettings)
    .settings(
      Test / fork                 := true,
      airframeHttpGeneratorOption := "-l trace",
      airframeHttpClients := Seq(
        "example.api:grpc"
      ),
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-grpc" % sys.props("airframe.version")
      )
    )
    .dependsOn(api)
