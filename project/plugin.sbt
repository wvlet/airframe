addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
//addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M14")
// sbt-scoverage doesn't work for Scala 2.12.0
//addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")
//addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")
scalacOptions ++= Seq("-deprecation", "-feature")
