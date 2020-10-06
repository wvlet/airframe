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
import wvlet.airframe.control.OS
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil.withResource
import scala.sys.process._

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
    val airframeHttpWorkDir         = settingKey[File]("working directory for airframe-http")
    val airframeHttpGenerateClient  = taskKey[Seq[File]]("Generate the client code")
    val airframeHttpGeneratorOption = settingKey[String]("airframe-http client-generator options")
    val airframeHttpClean           = taskKey[Unit]("clean artifacts")
    val airframeHttpClasspass       = taskKey[Seq[String]]("class loader for dependent classes")
    val airframeHttpBinaryDir       = taskKey[File]("Download Airframe HTTP binary to this location")
    val airframeHttpVersion         = settingKey[String]("airframe-http version to use")
    val airframeHttpReload          = taskKey[Seq[File]]("refresh generated clients")
    val airframeHttpOpts            = settingKey[String]("additional option for airframe-http commands")

    // Keys for OpenAPI spec generator
    val airframeHttpOpenAPIConfig    = settingKey[OpenAPIConfig]("OpenAPI spec generator configuration")
    val airframeHttpOpenAPIPackages  = settingKey[Seq[String]]("OpenAPI target API package names")
    val airframeHttpOpenAPITargetDir = settingKey[File]("OpenAPI spec file target folder")
    val airframeHttpOpenAPIGenerate  = taskKey[Seq[File]]("Generate OpenAPI spec from RPC definition")
  }

  private def dependentProjects: ScopeFilter =
    ScopeFilter(inDependencies(ThisProject, transitive = true, includeRoot = false))

  private val seqFileCodec  = MessageCodec.of[Seq[File]]
  private val cacheFileName = "generated.cache"

  def httpProjectSettings = {
    import sbt.Keys._
    Seq(
      airframeHttpClients := Seq.empty,
      airframeHttpClasspass := {
        // Compile all dependent projects
        (compile in Compile).all(dependentProjects).value

        val baseDir = (ThisBuild / baseDirectory).value
        val classpaths =
          ((Compile / dependencyClasspath).value.files :+ (Compile / classDirectory).value)
            .map { p =>
              p.relativeTo(baseDir).getOrElse(p).getPath
            }

        classpaths
      },
      airframeHttpWorkDir := (Compile / target).value / s"scala-${scalaBinaryVersion.value}" / s"airframe" / airframeHttpVersion.value,
      airframeHttpClean := {
        val d = airframeHttpWorkDir.value
        if (d.exists) {
          IO.delete(d)
        }
      },
      airframeHttpVersion := wvlet.airframe.sbt.BuildInfo.version,
      airframeHttpBinaryDir := {
        // This task is for downloading airframe-http library to parse Airframe HTTP/RPC interfaces using a forked JVM.
        // Without forking JVM, sbt's class loader cannot load @RPC and @Endpoint annotations.
        val airframeVersion        = airframeHttpVersion.value
        val airframeHttpPackageDir = airframeHttpWorkDir.value / "local"

        val versionFile = airframeHttpPackageDir / "VERSION"
        val needsUpdate = !versionFile.exists() ||
          !IO.readLines(versionFile).exists { line =>
            line.contains(s"version:=${airframeVersion}")
          }

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
          files.headOption.map { tgz =>
            // Extract tar.gz archive using commons-compress library
            info(s"Extracting airframe-http ${airframeVersion} package to ${airframeHttpPackageDir}")
            withResource(new GZIPInputStream(new FileInputStream(tgz))) { in =>
              val tgzInput = new TarArchiveInputStream(in)
              Iterator
                .continually(tgzInput.getNextTarEntry)
                .takeWhile(entry => entry != null)
                .filter(tgzInput.canReadEntryData(_))
                .foreach { entry =>
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
      airframeHttpGeneratorOption := "",
      airframeHttpReload := Def
        .sequential(
          Def.task {
            val targetDir: File = airframeHttpWorkDir.value
            val cacheFile       = targetDir / cacheFileName
            IO.delete(cacheFile)
          },
          airframeHttpGenerateClient
        ).value,
      airframeHttpGenerateClient := {
        val targetDir: File = airframeHttpWorkDir.value
        val cacheFile       = targetDir / cacheFileName
        val binDir          = airframeHttpBinaryDir.value
        val cp              = airframeHttpClasspass.value.mkString(":")
        val opts            = s"${airframeHttpOpts.value} ${airframeHttpGeneratorOption.value}"

        val result: Seq[File] = if (!cacheFile.exists) {
          debug(s"airframe-http directory: ${binDir}")
          val outDir: String = (Compile / sourceManaged).value.getPath
          val cmd =
            s"${binDir}/bin/${generatorName} generate ${opts} -cp ${cp} -o ${outDir} -t ${targetDir.getPath} ${airframeHttpClients.value
              .mkString(" ")}"
          debug(cmd)
          val json: String = cmd.!!
          debug(s"client generator result: ${json}")
          IO.write(cacheFile, json)
          // Return generated files
          seqFileCodec.unpackJson(json).getOrElse(Seq.empty)
        } else {
          debug(s"Using cached client")
          val json = IO.read(cacheFile)
          seqFileCodec.fromJson(json)
        }
        result
      },
      airframeHttpOpts := "",
      airframeHttpOpenAPIConfig := OpenAPIConfig(
        title = name.value,
        version = version.value
      ),
      airframeHttpOpenAPITargetDir := target.value,
      airframeHttpOpenAPIPackages := Seq.empty,
      airframeHttpOpenAPIGenerate := Def
        .task {
          val config             = airframeHttpOpenAPIConfig.value
          val formatType: String = config.format
          val outFile: File      = airframeHttpOpenAPITargetDir.value / s"${config.filePrefix}.${formatType}"
          val binDir: File       = airframeHttpBinaryDir.value
          val cp                 = airframeHttpClasspass.value.mkString(":")
          val packages           = airframeHttpOpenAPIPackages.value
          val opts               = airframeHttpOpts.value
          if (packages.isEmpty) {
            Seq.empty
          } else {
            // Build command line manually because scala.sys.process cannot parse quoted strings
            val cmd = Seq.newBuilder[String]
            cmd += s"${binDir}/bin/${generatorName}"
            cmd += "openapi"
            if (opts.nonEmpty) {
              cmd ++= opts.split("\\s+")
            }
            cmd ++= Seq(
              "-cp",
              cp,
              "-f",
              formatType,
              "-o",
              outFile.getPath,
              "--title",
              config.title,
              "--version",
              config.version
            )
            cmd ++= packages

            val cmdline = cmd.result()
            debug(cmdline)
            Process(cmdline).!!
            Seq(outFile)
          }
        }.dependsOn(Compile / compile).value,
      // Generate HTTP clients before compilation
      Compile / sourceGenerators += airframeHttpGenerateClient,
      // Generate OpenAPI doc when generating package
      Compile / `package` := (Compile / `package`).dependsOn(airframeHttpOpenAPIGenerate).value
    )
  }

  private def generatorName = {
    val cmdName = if (OS.isWindows) {
      "airframe-http-code-generator.bat"
    } else {
      "airframe-http-code-generator"
    }
    cmdName
  }

}
