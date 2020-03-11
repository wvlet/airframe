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

import java.net.URLClassLoader

import sbt.Keys._
import sbt._
import wvlet.airframe.http.codegen._
import wvlet.airframe.http.Router
import wvlet.airframe.http.codegen.HttpClientGeneratorConfig
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

  object autoImport extends AirframeHttpKeys
  import autoImport._

  override def requires: Plugins = plugins.JvmPlugin
  override def trigger           = noTrigger

  override def projectSettings = httpProjectSettings

  sealed trait ClientType
  case object AsyncClient   extends ClientType
  case object SyncClient    extends ClientType
  case object ScalaJSClient extends ClientType

  trait AirframeHttpKeys {
    val airframeHttpPackages                  = settingKey[Seq[String]]("A list of package names containing Airframe HTTP interfaces")
    val airframeHttpTargetPackage             = settingKey[String]("Generate target package name for the generated code")
    val airframeHttpClientType                = settingKey[ClientType]("Client type to generate")
    val airframeHttpGenerateClient            = taskKey[Seq[File]]("Generate the client code")
    private[http] val airframeHttpRouter      = taskKey[Router]("Airframe Router")
    private[http] val airframeHttpClassLoader = taskKey[URLClassLoader]("class loader for dependent classes")
  }

  private def dependentProjects: ScopeFilter =
    ScopeFilter(inDependencies(ThisProject, transitive = true, includeRoot = false))

  def httpProjectSettings =
    Seq(
      airframeHttpPackages := Seq(),
      airframeHttpTargetPackage := "generated",
      airframeHttpClassLoader := {
        // Compile all dependent projects
        (compile in Compile).all(dependentProjects).value

        val urls = Seq.newBuilder[URL]
        (dependencyClasspath in Compile).value.files.foreach { f => urls += f.toURI.toURL }
        val cp = urls.result()
        val cl = new URLClassLoader(cp.toArray, getClass().getClassLoader)
        cl
      },
      airframeHttpRouter := {
        val router = RouteScanner.buildRouter(airframeHttpPackages.value, airframeHttpClassLoader.value)
        info(router)
        router
      },
      airframeHttpClientType := AsyncClient,
      airframeHttpGenerateClient := {
        val router = airframeHttpRouter.value
        val config = HttpClientGeneratorConfig(packageName = airframeHttpTargetPackage.value)

        val path            = config.packageName.replaceAll("\\.", "/")
        val file: File      = (Compile / sourceManaged).value / path / s"${config.className}.scala"
        val baseDir         = (ThisBuild / baseDirectory).value
        val relativeFileLoc = file.relativeTo(baseDir).getOrElse(file)

        val clientType = airframeHttpClientType.value
        info(s"Generating http client code for ${clientType}: ${relativeFileLoc}")
        val code = HttpClientGenerator.generate(router, clientType.toString, config)
        IO.write(file, code)
        Seq(file)
      },
      Compile / sourceGenerators += Def.task {
        airframeHttpGenerateClient.value
      }.taskValue
    )

}
