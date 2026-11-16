import sbt.*
import sbt.Keys.*

/**
  * Shrinks the full Scaladoc of a published artifact by rendering its side menu dynamically.
  *
  * Why: Maven Central caps how much each organization can publish per month
  * (https://central.sonatype.org/publish/maven-central-publishing-limits/); see MinimizeJavadoc.
  *
  * By default Scaladoc 3 embeds the whole navigation side menu in every HTML page (~100 KB per page).
  * `-dynamic-side-menu` writes the menu once to dynamicSideMenu.json and renders it with JS. Scala 2 scaladoc has no
  * such option, so this applies only to Scala 3.
  *
  * Scaladoc up to 3.10 builds broken menu links unless the docs are served from the root of a host, so the links fail
  * on javadoc.io (/doc/<org>/<artifact>/<version>/...). The bug, and the fix patched in below, is scala/scala3#27403.
  * Remove the patch once airframe builds with a Scala release that includes that fix.
  */
object MinimizeScaladoc {
  // In scripts/ux.js, baseUrl is an array of path segments, so `${baseUrl}` joins them with commas
  private val brokenLink = "`${baseUrl}/${loc.join(\"/\")}.html`"
  private val fixedLink  = "`${baseUrl.join(\"/\")}/${loc.join(\"/\")}.html`"

  val minimizeScaladoc: Seq[Setting[?]] = Seq(
    Compile / doc / scalacOptions ++= {
      if scalaVersion.value.startsWith("3.") then Seq("-dynamic-side-menu") else Seq.empty
    },
    // Def.uncached: the task writes the patched ux.js, which a cached result would not restore
    Compile / packageDoc / mappings :=
      Def.uncached {
        val converter = fileConverter.value
        val log       = streams.value.log
        val patched   = (Compile / target).value / "scaladoc-patch" / "ux.js"
        (Compile / packageDoc / mappings).value
          .map {
            case (ref, path @ "scripts/ux.js") =>
              val js = IO.read(converter.toPath(ref).toFile)
              if !js.contains(brokenLink) then {
                log.warn(
                  "Scaladoc's ux.js no longer has the side menu link bug (scala/scala3#27403); " +
                    "remove the patch in project/MinimizeScaladoc.scala"
                )
              }
              IO.write(patched, js.replace(brokenLink, fixedLink))
              converter.toVirtualFile(patched.toPath) -> path
            case m =>
              m
          }
      }
  )
}
