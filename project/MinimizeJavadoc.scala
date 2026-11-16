import sbt.*
import sbt.Keys.*

/**
  * Publishes the full Scaladoc only for one artifact per module, and a small link page for the others.
  *
  * Why: Maven Central caps how much each organization can publish per month
  * (https://central.sonatype.org/publish/maven-central-publishing-limits/), and the `wvlet` organization is shared by
  * airframe and uni. A module is cross-built for Scala 2.12, 2.13 and 3 on the JVM, Scala.js and Scala Native, and each
  * of these -javadoc.jar files repeated nearly the same API. Maven Central still requires a -javadoc.jar for every
  * artifact, so the other artifacts publish (and sign) a jar with only index.html and README.md. Skipping scaladoc also
  * makes releases faster.
  *
  * By default the full Scaladoc is kept for the JVM artifact built with the first (newest) entry of
  * `crossScalaVersions`, e.g. airframe-log_3, or airframe-http-finagle_2.13 for a Scala 2 only module. Use
  * `jsOnlyModule` for a module without a JVM artifact, and `minimizePluginJavadoc` for sbt plugins.
  *
  * The sbt-airframe and airspec builds compile this file into their metabuild (see their project/plugin.sbt).
  */
object MinimizeJavadoc {
  val fullScaladoc         = settingKey[Boolean]("Publish the full Scaladoc instead of a link page")
  val fullScaladocArtifact =
    settingKey[String]("The artifact whose -javadoc.jar has the full Scaladoc of this module")
  val javadocLinkPage = settingKey[LinkPage]("The content of the -javadoc.jar without full Scaladoc")

  case class LinkPage(title: String, intro: String, links: Seq[(String, String)])

  private val siteUrl = "https://wvlet.org/airframe/"

  private def newestScalaBinaryVersion = Def.setting {
    CrossVersion.binaryScalaVersion(crossScalaVersions.value.headOption.getOrElse(scalaVersion.value))
  }

  private def isNewestScalaVersion = Def.setting {
    crossScalaVersions.value.headOption.forall(_ == scalaVersion.value)
  }

  /**
    * Add to the settings of every published project.
    */
  val settings: Seq[Setting[?]] = Seq(
    // platform is jvm, sjs1 (Scala.js) or native0.5 (Scala Native)
    fullScaladoc         := platform.value == "jvm" && isNewestScalaVersion.value,
    fullScaladocArtifact := s"${moduleName.value}_${newestScalaBinaryVersion.value}",
    javadocLinkPage      := {
      val canonical = fullScaladocArtifact.value
      val docsUrl   = s"https://javadoc.io/doc/${organization.value}/${canonical}/${version.value}"
      LinkPage(
        title = s"${moduleName.value} API documentation",
        intro =
          s"This artifact shares its API with `${canonical}`. To keep releases small, the Scaladoc is published only with that artifact:",
        links = Seq("" -> docsUrl, "Documentation: " -> siteUrl)
      )
    },
    // Skip running scaladoc for the link page
    Compile / doc / sources := {
      if fullScaladoc.value then (Compile / doc / sources).value else Seq.empty
    },
    // Def.uncached: the task writes the link page, which a cached result would not restore
    Compile / packageDoc / mappings :=
      Def.uncached {
        val full     = fullScaladoc.value
        val docFiles = (Compile / packageDoc / mappings).value
        if full then docFiles
        else {
          val converter = fileConverter.value
          val dir       = (Compile / target).value / "javadoc-link-page"
          val page      = javadocLinkPage.value
          IO.delete(dir)
          IO.write(dir / "index.html", indexHtml(page))
          IO.write(dir / "README.md", readme(page))
          Seq("index.html", "README.md").map { f =>
            converter.toVirtualFile((dir / f).toPath) -> f
          }
        }
      }
  )

  /**
    * For a Scala.js only module: keep the full Scaladoc for the newest Scala version.
    */
  val jsOnlyModule: Seq[Setting[?]] = Seq(
    fullScaladoc         := isNewestScalaVersion.value,
    fullScaladocArtifact := s"${moduleName.value}_${platform.value}_${newestScalaBinaryVersion.value}"
  )

  /**
    * For an sbt plugin, which has no other artifact with its Scaladoc. The page links to the guide and the source code.
    *
    * @param guide
    *   the guide page under https://wvlet.org/airframe/, e.g. "docs/airframe-rpc"
    * @param sourceDir
    *   the source directory in the airframe repository, e.g. "sbt-airframe/src/main/scala"
    */
  def minimizePluginJavadoc(guide: String, sourceDir: String): Seq[Setting[?]] =
    settings ++ Seq(
      fullScaladoc    := false,
      javadocLinkPage := {
        val ref = if isSnapshot.value then "main" else s"v${version.value}"
        LinkPage(
          title = s"${moduleName.value} API documentation",
          intro = "To keep releases small, the Scaladoc of this artifact is not published. See:",
          links = Seq(
            "Guide: "       -> s"${siteUrl}${guide}",
            "Source code: " -> s"https://github.com/wvlet/airframe/tree/${ref}/${sourceDir}"
          )
        )
      }
    )

  private def indexHtml(page: LinkPage): String = {
    val introHtml = page.intro.replaceAll("`([^`]+)`", "<code>$1</code>")
    val items     = page.links
      .map { case (label, url) => s"""<li>${label}<a href="${url}">${url}</a></li>""" }
      .mkString("\n")
    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |<meta charset="utf-8">
       |<title>${page.title}</title>
       |</head>
       |<body>
       |<h1>API documentation</h1>
       |<p>${introHtml}</p>
       |<ul>
       |${items}
       |</ul>
       |</body>
       |</html>
       |""".stripMargin
  }

  private def readme(page: LinkPage): String =
    s"""# API documentation
       |
       |${page.intro}
       |
       |${page.links.map { case (label, url) => s"- ${label}${url}" }.mkString("\n")}
       |""".stripMargin
}
