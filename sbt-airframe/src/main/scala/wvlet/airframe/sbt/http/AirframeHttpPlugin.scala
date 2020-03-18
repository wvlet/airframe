/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.sbt.http

import coursier.core.{Extension, Publication}
import sbt._
import wvlet.airframe.codec.MessageCodec
import wvlet.log.LogSupport

/**
  * sbt plugin for supporting Airframe HTTP development.
  *
  * This plugin supports:
  * - Building a Router by scanning interfaces that have methods with @Endpoint annotations in the project
  * - Generate HTTP client code for Scala and Scala.js
  *
  */
object AirframeHttpPlugin extends AutoPlugin with LogSupport {
  wvlet.airframe.log.init

  object autoImport extends Keys
  import autoImport._

  override def requires: Plugins = sbt.plugins.JvmPlugin
  override def trigger           = noTrigger

  override def projectSettings = httpProjectSettings

  trait Keys {
    val airframeHttpClients = settingKey[Seq[String]](
      "HTTP client generator targets, <api package name>(:<client type>(:<target package name>)?)?"
    )
    val airframeHttpGenerateClient = taskKey[Seq[File]]("Generate the client code")
    val airframeHttpClean          = taskKey[Unit]("clean artifacts")
    val airframeHttpClasspass      = taskKey[Seq[String]]("class loader for dependent classes")
    val airframeHttpBinaryDir      = taskKey[File]("Download Airframe HTTP Binary")
    val airframeHttpVersion        = settingKey[String]("airframe-http version")
  }

  private def dependentProjects: ScopeFilter =
    ScopeFilter(inDependencies(ThisProject, transitive = true, includeRoot = false))

  def httpProjectSettings = {
    import sbt.Keys._
    Seq(
      airframeHttpClients := Seq.empty,
      airframeHttpClasspass := {
        // Compile all dependent projects
        (compile in Compile).all(dependentProjects).value

        val baseDir = (ThisBuild / baseDirectory).value
        val classpaths = (dependencyClasspath in Compile).value.files
          .map { p => p.relativeTo(baseDir).getOrElse(p).getPath }
        classpaths
      },
      airframeHttpClean := {
//        for (targetClient <- airframeHttpClients.value) {
//          val config     = HttpClientGeneratorConfig(targetClient)
//          val path       = config.targetPackageName.replaceAll("\\.", "/")
//          val file: File = (Compile / sourceManaged).value / path / s"${config.className}.scala"
//          IO.delete(file)
//        }
      },
      airframeHttpVersion := wvlet.airframe.sbt.BuildInfo.version,
      airframeHttpBinaryDir := {
        val airframeVersion = airframeHttpVersion.value
        val targetDir       = (Compile / target).value / scalaBinaryVersion.value / "airframe-http"

        val versionFile = targetDir / "VERSION"
        if (!(versionFile.exists() && IO
              .readLines(versionFile).exists { line => line.startsWith("version:=") && line.endsWith(airframeVersion) })) {
          import coursier._

          val moduleName = s"airframe-http_${scalaBinaryVersion.value}"
          val d = new Dependency(
            module = Module(
              Organization("org.wvlet.airframe"),
              ModuleName(moduleName)
            ),
            version = airframeHttpVersion.value,
            configuration = coursier.core.Configuration.empty,
            exclusions = Set.empty,
            publication = Publication("", Type("arch"), Extension("tar.gz"), coursier.Classifier.empty),
            optional = false,
            transitive = false
          )
          debug(s"Downloading ${d} with Coursier")

          val files =
            Fetch()
              .addDependencies(d)
              .allArtifactTypes()
              .run()

          targetDir.mkdirs()
          files.headOption.map { tgz =>
            import scala.sys.process._
            // TODO: Use pure-java tar.gz unarchiver
            val cmd = s"tar xvfz ${tgz.getAbsolutePath} --strip-components 1 -C ${targetDir.getAbsolutePath}/"
            debug(cmd)
            cmd.!!
          }
        }
        targetDir
      },
      airframeHttpGenerateClient := {
        val binDir = airframeHttpBinaryDir.value
        info(s"airframe-http dir: ${binDir}")
        val cp = airframeHttpClasspass.value.mkString(":")

        val outDir: String    = (Compile / sourceManaged).value.getPath
        val targetDir: String = (Compile / target).value.getPath
        val cmd =
          s"${binDir}/bin/airframe-http-client-generator generate -cp ${cp} -o ${outDir} -t ${targetDir} ${airframeHttpClients.value
            .mkString(" ")}"
        debug(cmd)
        import scala.sys.process._
        val json: String = cmd.!!
        info(json)

        MessageCodec.fromJson[Seq[File]](json)
      },
      Compile / sourceGenerators += Def.task {
        airframeHttpGenerateClient.value
      }.taskValue
    )
  }
}
