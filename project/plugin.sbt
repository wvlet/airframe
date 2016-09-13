addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
// sbt-scoverage doesn't work for Scala 2.12.0
//addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")
//addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")
scalacOptions ++= Seq("-deprecation", "-feature")
