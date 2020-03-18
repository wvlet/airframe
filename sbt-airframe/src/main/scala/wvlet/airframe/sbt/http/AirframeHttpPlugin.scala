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
    val airframeHttpBinary         = taskKey[Seq[File]]("Download Airframe HTTP Binary")
    val airframeHttpVersion        = taskKey[String]("airframe-http version")
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

        val urls = Seq.newBuilder[URL]
        (dependencyClasspath in Compile).value.files.foreach { f => urls += f.toURI.toURL }
        val cp = urls.result()
        cp.map(_.getPath).toSeq
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
      airframeHttpBinary := {
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
        info(s"Download ${d}")

        val files =
          Fetch()
            .addDependencies(d) //dep"org.wvlet.airframe:airframe-http_2.12:20.3.0+38-7a83cf4b+20200317-2205-SNAPSHOT")
            .allArtifactTypes()
            .run()

        files
      },
      airframeHttpGenerateClient := {
        val files = airframeHttpBinary.value
        info(s"binary files: ${files.mkString("\n")}")
//        val cl = airframeHttpClassLoader.value
//        val generatedFiles = for (targetClient <- airframeHttpClients.value) yield {
//          val config = HttpClientGeneratorConfig(targetClient)
//          val router = RouteScanner.buildRouter(Seq(config.apiPackageName), cl)
//
//          val routerHash           = router.toString.hashCode
//          val routerHashFile: File = (Compile / target).value / f"router-${routerHash}%07x.update"
//          info(s"Found router ${routerHashFile}:\n${router}")
//          val path            = config.targetPackageName.replaceAll("\\.", "/")
//          val file: File      = (Compile / sourceManaged).value / path / s"${config.className}.scala"
//          val baseDir         = (ThisBuild / baseDirectory).value
//          val relativeFileLoc = file.relativeTo(baseDir).getOrElse(file)
//
//          if (!(file.exists() && routerHashFile.exists())) {
//            info(s"Found an airframe-http router for package ${config.apiPackageName}:\n${router}")
//            info(s"Generating a ${config.clientType.name} client code: ${relativeFileLoc}")
//            val code = HttpClientGenerator.generate(router, config)
//            IO.write(file, code)
//            IO.touch(routerHashFile)
//          } else {
//            info(s"${relativeFileLoc} is up-to-date")
//          }
//          file
//        }
//        generatedFiles
        Seq.empty
      },
      Compile / sourceGenerators += Def.task {
        airframeHttpGenerateClient.value
      }.taskValue
    )
  }

}
