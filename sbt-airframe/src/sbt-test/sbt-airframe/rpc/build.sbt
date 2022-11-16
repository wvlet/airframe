import wvlet.airframe.sbt.http.AirframeHttpPlugin

// Workaround for com.twitter:util-core_2.12:21.4.0 (depends on 1.1.2)
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "always"

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

val AIRSPEC_VERSION = "22.11.1"

val buildSettings: Seq[Def.Setting[_]] = Seq(
  testFrameworks += new TestFramework("wvlet.airspec.Framework"),
  libraryDependencies += "org.wvlet.airframe" %% "airspec" % AIRSPEC_VERSION % Test
)

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
        "org.wvlet.airframe" %% "airframe-http-finagle" % sys.props("airframe.version")
      )
    )
    .dependsOn(api)
