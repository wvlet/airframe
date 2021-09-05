import wvlet.airframe.sbt.http.AirframeHttpPlugin

// Workaround for com.twitter:util-core_2.12:21.4.0 (depends on 1.1.2)
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "always"

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

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
        "myapp.spi",
        "myapp.spi:sync"
      ),
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-http-finagle" % sys.props("airframe.version")
      )
    )
    .dependsOn(spi)
