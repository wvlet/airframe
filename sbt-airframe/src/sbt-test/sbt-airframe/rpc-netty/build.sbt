import wvlet.airframe.sbt.http.AirframeHttpPlugin

// Workaround for com.twitter:util-core_2.12:21.4.0 (depends on 1.1.2)
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "always"

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

val AIRSPEC_VERSION = sys.env.getOrElse("AIRSPEC_VERSION", "23.5.5")

val buildSettings: Seq[Def.Setting[_]] = Seq(
  testFrameworks += new TestFramework("wvlet.airspec.Framework"),
  libraryDependencies += "org.wvlet.airframe" %% "airspec" % AIRSPEC_VERSION % Test
)

ThisBuild / scalaVersion := "3.3.0"

lazy val root =
  project.aggregate(api, server)

lazy val api =
  project
    .in(file("api"))
    .settings(
      buildSettings,
      libraryDependencies += "org.wvlet.airframe" %% "airframe-http" % sys.props("airframe.version")
    )

lazy val server =
  project
    .in(file("server"))
    .enablePlugins(AirframeHttpPlugin)
    .settings(
      buildSettings,
      airframeHttpGeneratorOption := "-l trace",
      airframeHttpClients := Seq(
        "example.api:rpc:example.api.MyRPCClient"
      ),
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-netty" % sys.props("airframe.version"),
        "org.wvlet.airframe" %% "airframe-rx"         % sys.props("airframe.version")
      )
    )
    .dependsOn(api)
