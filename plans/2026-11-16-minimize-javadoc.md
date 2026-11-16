# Minimize -javadoc.jar for Maven Central publishing limits

## Goals

- Keep airframe releases well within Maven Central's monthly per-organization limits. The `wvlet` org is shared with uni, and uni v2026.2.0 was already blocked.
- Publish one full Scaladoc per module and make every other `-javadoc.jar` a small link page. Maven Central still requires a javadoc jar for each artifact.
- Shrink the full Scaladoc that remains.

## Background

- Airframe 2026.2.5 published 142 javadoc jars totalling 304 MB on Maven Central: JVM `_3` 83 MB, `_2.13` 41 MB, `_2.12` 41 MB, `_sjs1_*` 96 MB, `_native0.5_3` 40 MB, sbt-airframe 2.7 MB. That is about 73% of the ~416 MB release.
- #4320 (sbt-salad-days) already strips fonts and inkuire.js: about −25% on Scala 3.3.9 and −74% on Scala 2. Exact "before" numbers for this PR come from a local build of main.
- The JVM, Scala 2, Scala.js and Native jars of a module document nearly the same API. airspec is a separate build in `airspec/` and is released from the same tag, with the same artifact matrix.
- Scaladoc 3.3.9 supports `-dynamic-side-menu`. Its `scripts/ux.js` has the same sub-path link bug as in uni (#927): the line `${baseUrl}/${loc.join("/")}.html` is identical. So uni's patch applies as is.
- Scala 2 scaladoc has no equivalent flag. It doesn't need one if the Scala 2 jars become link pages.

## Design

Lane: **Standard**. It changes what users see on javadoc.io for most airframe artifacts, which is a policy choice.

- **Canonical full Scaladoc = the JVM artifact at the newest Scala version the module builds for.** For most modules that is `<module>_3`.
  - Scala-2-only modules (airframe-http-finagle) keep `_2.13`.
  - JS-only airframe-rx-widget keeps `_sjs1_3`.
  - The rule is "JVM platform and `scalaVersion == crossScalaVersions.head`", so new modules follow it without extra settings.
- **Every other jar is a link page** (`index.html` + `README.md`, ~1 KB): `_2.12`, `_2.13`, all `_sjs1_*` and `_native0.5_3`. It links to the canonical artifact on javadoc.io and to https://wvlet.org/airframe/. Scaladoc doesn't run for these, which also speeds up releases.
- **Full Scaladoc gets `-dynamic-side-menu` and the ux.js patch** (uni's `minimizeScaladoc`). Without the flag, Scaladoc 3 embeds the ~100 KB nav menu in every page. The patch logs a warning once Scaladoc ships the upstream fix (scala/scala3#27403).
- **sbt-airframe** publishes a link page to its guide (`docs/airframe-rpc`) and its GitHub source (tag `v<version>`, or `main` for snapshots), as in uni #928.
- **airspec** gets the same policy: `airspec_3` keeps full Scaladoc, the others become link pages.
- **Code**: `project/MinimizeJavadoc.scala` and `project/MinimizeScaladoc.scala`, adapted from uni. The `sbt-airframe` and `airspec` builds compile the root files through `Compile / unmanagedSources` in their `project/plugin.sbt`. sbt-salad-days stays wherever full Scaladoc still runs and is dropped from sbt-airframe.
- **Validation**: measure every compressed `-javadoc.jar` before and after, across all Scala versions and platforms plus sbt-airframe and airspec, and put a per-release total in the PR body. Unzip, serve over HTTP, and load the pages headlessly at the host root and under a javadoc.io-style sub-path.

## Alternatives and Why Not?

- **Canonical = `_2.13`**: Scala 2 users would get exact docs, but Scala 3 is the default build and gets `-dynamic-side-menu`. Scala 2 docs can't be shrunk the same way.
- **Keep full Scaladoc for every Scala version (JVM only)**: still about 20+ MB of near-duplicate Scala 2 docs per release, for little reader value.
- **Keep Scala.js full for its JS-only APIs** (e.g. `rx-html` DOM helpers): the shared API dominates. `rx-html_3` on the JVM documents the same DSL, and rx-widget keeps its own.
