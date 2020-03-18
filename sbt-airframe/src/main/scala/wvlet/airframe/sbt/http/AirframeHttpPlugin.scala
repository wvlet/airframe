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

import java.io.FileInputStream
import java.nio.file.Files
import java.util.zip.GZIPInputStream

import coursier.core.{Extension, Publication}
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import sbt._
import wvlet.airframe.codec.MessageCodec
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil.withResource

/**
  * sbt plugin for supporting Airframe HTTP development.
  *
  * This plugin supports:
  * - Building a Router by scanning interfaces that have methods with @Endpoint annotations in the project
  * - Generate HTTP client code for Scala and Scala.js.
  *
  * The client code generator is defined in wvlet.airframe.http.codegen package.
  * This plugin downloads a pre-built archive (airframe-http_(scala version)-(airframe version).tgz) and
  * invoke HttpClientGenerator.
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
        val airframeVersion        = airframeHttpVersion.value
        val airframeHttpPackageDir = (Compile / target).value / scalaBinaryVersion.value / "airframe-http"

        val versionFile = airframeHttpPackageDir / "VERSION"
        val needsUpdate = !versionFile.exists() ||
          IO.readLines(versionFile).exists { line => line.contains(s"version:=${airframeVersion}") }

        if (needsUpdate) {
          // Download airframe-http.tgz with coursier
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
              .allArtifactTypes() // This line is necessary to choose a specific publication (arch, tar.gz)
              .run()

          // Unpack .tgz file
          val packageDir = airframeHttpPackageDir.getAbsoluteFile
          airframeHttpPackageDir.mkdirs()
          files.headOption.map {
            tgz =>
              // Extract tar.gz archive using commons-compress library
              withResource(new GZIPInputStream(new FileInputStream(tgz))) {
                in =>
                  val tgzInput = new TarArchiveInputStream(in)
                  Iterator
                    .continually(tgzInput.getNextTarEntry)
                    .takeWhile(entry => entry != null)
                    .filter(tgzInput.canReadEntryData(_))
                    .foreach {
                      entry =>
                        val fileName     = entry.getName
                        val mode         = entry.getMode
                        val isExecutable = (mode & (1 << 6)) != 0

                        // Strip the first path component
                        val path       = fileName.split("/").tail.mkString("/")
                        val outputFile = new File(packageDir, path)
                        if (entry.isDirectory) {
                          debug(s"Creating dir : ${path}")
                          outputFile.mkdirs()
                        } else {
                          withResource(Files.newOutputStream(outputFile.toPath)) { out =>
                            debug(s"Creating file: ${path}")
                            IOUtils.copy(tgzInput, out)
                          }
                          // Set +x for executables
                          outputFile.setExecutable(isExecutable)
                        }
                    }
              }
          }
        }
        airframeHttpPackageDir
      },
      airframeHttpGenerateClient := {
        val binDir = airframeHttpBinaryDir.value
        info(s"airframe-http directory: ${binDir}")
        val cp = airframeHttpClasspass.value.mkString(":")

        val outDir: String    = (Compile / sourceManaged).value.getPath
        val targetDir: String = (Compile / target).value.getPath
        val cmd =
          s"${binDir}/bin/airframe-http-client-generator generate -cp ${cp} -o ${outDir} -t ${targetDir} ${airframeHttpClients.value
            .mkString(" ")}"
        debug(cmd)
        import scala.sys.process._
        val json: String = cmd.!!
        debug(s"client generator result: ${json}")
        // Return generated files
        MessageCodec.of[Seq[File]].unpackJson(json).getOrElse(Seq.empty)
      },
      Compile / sourceGenerators += Def.task {
        airframeHttpGenerateClient.value
      }.taskValue
    )
  }
}
