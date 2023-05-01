import wvlet.airframe.sbt.http.AirframeHttpPlugin

// Workaround for com.twitter:util-core_2.12:21.4.0 (depends on 1.1.2)
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "always"

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

ThisBuild / scalaVersion := "3.2.2"

lazy val root =
  project.aggregate(spi, server)

lazy val spi =
  project
    .in(file("spi"))
    .settings(
      libraryDependencies += "org.wvlet.airframe" %% "airframe-http" % sys.props("airframe.version")
    )

lazy val server =
  project
    .in(file("server"))
    .enablePlugins(AirframeHttpPlugin)
    .settings(
      airframeHttpGeneratorOption := "-l trace",
      airframeHttpClients := Seq(
        "myapp.spi:rpc"
      ),
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-netty" % sys.props("airframe.version")
      )
    )
    .dependsOn(spi)
