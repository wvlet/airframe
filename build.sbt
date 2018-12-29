val SCALA_2_12 = "2.12.8"
val SCALA_2_13 = "2.13.0-M5"

val targetScalaVersions = Seq(SCALA_2_12, SCALA_2_13)

val SCALATEST_VERSION               = "3.0.6-SNAP4"
val SCALACHECK_VERSION              = "1.14.0"
val SCALA_PARSER_COMBINATOR_VERSION = "1.1.1"

val AIRFRAME_VERSION = "0.78"

// Allow using Ctrl+C in sbt without exiting the prompt
cancelable in Global := true

// For using Scala 2.12 in sbt
scalaVersion in ThisBuild := SCALA_2_12
organization in ThisBuild := "org.wvlet.msgframe"

val isTravisBuild: Boolean = sys.env.isDefinedAt("TRAVIS")
// In release process, this environment variable should be set
val isRelease: Boolean = sys.env.isDefinedAt("RELEASE")

// Use dynamic snapshot version strings for non tagged versions
dynverSonatypeSnapshots in ThisBuild := !isRelease

// For publishing in Travis CI
lazy val travisSettings = List(
  // For publishing on Travis CI
  useGpg := false,
  usePgpKeyHex("42575E0CCD6BA16A"),
  pgpPublicRing := file("./travis/local.pubring.asc"),
  pgpSecretRing := file("./travis/local.secring.asc"),
  // PGP_PASS, SONATYPE_USER, SONATYPE_PASS are encoded in .travis.yml
  pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray),
  credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    sys.env.getOrElse("SONATYPE_USER", ""),
    sys.env.getOrElse("SONATYPE_PASS", "")
  )
)

inThisBuild(if (isTravisBuild) travisSettings else List.empty)

val buildSettings = Seq[Setting[_]](
  scalaVersion := SCALA_2_12,
  crossScalaVersions := targetScalaVersions,
  crossPaths := true,
  publishMavenStyle := true,
  logBuffered in Test := false,
  scalacOptions ++= Seq("-feature", "-deprecation"), // ,"-Ytyper-debug"),
  sonatypeProfileName := "org.wvlet",
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/wvlet/msgframe")),
  scmInfo := Some(
    ScmInfo(
      browseUrl = url("https://github.com/wvlet/msgframe"),
      connection = "scm:git@github.com:wvlet/msgframe.git"
    )
  ),
  developers := List(
    Developer(id = "leo", name = "Taro L. Saito", email = "leo@xerial.org", url = url("http://xerial.org/leo"))
  ),
  // Use sonatype resolvers
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases")
  )
)

// We need to define this globally as a workaround for https://github.com/sbt/sbt/pull/3760
publishTo in ThisBuild := sonatypePublishTo.value

val noPublish = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

lazy val root =
  project
    .in(file("."))
    .settings(name := "msgframe-root")
    .settings(buildSettings)
    .settings(noPublish)
    .aggregate(scaladoc)
    .aggregate(jvmProjects: _*)

lazy val scaladoc =
  project
    .enablePlugins(ScalaUnidocPlugin)
    .in(file("msgframe-scaladoc"))
    .settings(
      buildSettings,
      crossScalaVersions := targetScalaVersions,
      name := "msgframe-scaladoc",
      // compile projects first
      Defaults.packageTaskSettings(packageDoc in Compile, (unidoc in Compile).map(_.flatMap(Path.allSubpaths)))
    )
    .aggregate(jvmProjects: _*)

lazy val jvmProjects: Seq[ProjectReference] = Seq[ProjectReference](
  sql
)

lazy val projectJVM =
  project
    .settings(
      noPublish,
      crossScalaVersions := targetScalaVersions
    )
    .aggregate(jvmProjects: _*)

lazy val docs =
  project
    .in(file("docs"))
    .settings(
      name := "docs",
      publishArtifact := false,
      publish := {},
      publishLocal := {},
      // Necessary for publishMicrosite
      git.remoteRepo := "git@github.com:wvlet/msgframe.git",
      ghpagesNoJekyll := false,
      micrositeName := "Msgframe",
      micrositeDescription := "Lightweight Building Blocks for Scala",
      micrositeAuthor := "Taro L. Saito",
      micrositeOrganizationHomepage := "https://github.com/wvlet",
      micrositeHighlightTheme := "ocean",
      micrositeGithubOwner := "wvlet",
      micrositeGithubRepo := "msgframe",
      micrositeUrl := "https://wvlet.org",
      micrositeBaseUrl := "msgframe",
      micrositeAnalyticsToken := "UA-98364158-1",
      micrositeDocumentationUrl := "docs",
      micrositeGitterChannel := true,
      micrositeGitterChannelUrl := "wvlet/msgframe",
      //micrositePushSiteWith := GitHub4s,
      //micrositeGithubToken := sys.env.get("GITHUB_REPO_TOKEN"),
      micrositePalette ++= Map(
        "brand-primary"   -> "#2582AA",
        "brand-secondary" -> "#143F56",
        "brand-tertiary"  -> "#042F46",
        "gray-dark"       -> "#453E46",
        "gray"            -> "#534F54"
      ),
      watchSources += new sbt.internal.io.Source(
        sourceDirectory.value,
        new FileFilter {
          def accept(f: File) = !f.isDirectory
        },
        NothingFilter
      )
    )
    .enablePlugins(MicrositesPlugin)

val airframeSpec = "org.wvlet.airframe" %% "airframe-spec" % AIRFRAME_VERSION % "test"

lazy val sql =
  project
    .enablePlugins(Antlr4Plugin)
    .in(file("msgframe-sql"))
    .settings(buildSettings)
    .settings(
      name := "msgframe-sql",
      description := "SQL parser & analyzer",
      antlr4Version in Antlr4 := "4.7.1",
      antlr4PackageName in Antlr4 := Some("wvlet.msgframe.sql.parser"),
      antlr4GenListener in Antlr4 := true,
      antlr4GenVisitor in Antlr4 := true,
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %% "airframe-msgpack" % AIRFRAME_VERSION,
        "org.wvlet.airframe" %% "airframe-surface" % AIRFRAME_VERSION,
        airframeSpec
      )
    )
