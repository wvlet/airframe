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
import wvlet.airframe.http.{Endpoint, Router}
import wvlet.airframe.sbt.http.HttpClientGenerator.ClientBuilderConfig
import wvlet.log.LogSupport

import scala.util.{Success, Try}

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
        val router = buildRouter(airframeHttpPackages.value, airframeHttpClassLoader.value)
        info(router)
        router
      },
      airframeHttpClientType := AsyncClient,
      airframeHttpGenerateClient := {
        val router = airframeHttpRouter.value
        val config = ClientBuilderConfig(packageName = airframeHttpTargetPackage.value)

        val path            = config.packageName.replaceAll("\\.", "/")
        val file: File      = (Compile / sourceManaged).value / path / s"${config.className}.scala"
        val baseDir         = (ThisBuild / baseDirectory).value
        val relativeFileLoc = file.relativeTo(baseDir).getOrElse(file)

        val code = airframeHttpClientType.value match {
          case AsyncClient =>
            info(s"Generating http client code for Scala: ${relativeFileLoc}")
            HttpClientGenerator.generateHttpClient(router, config)
          case SyncClient => throw new NotImplementedError("SyncClient is not yet supported")
          case ScalaJSClient =>
            info(s"Generating http client code for Scala.js: ${relativeFileLoc}")
            HttpClientGenerator.generateScalaJsHttpClient(router, config)
        }
        IO.write(file, code)
        Seq(file)
      },
      Compile / sourceGenerators += Def.task {
        airframeHttpGenerateClient.value
      }.taskValue
    )

  /**
    * Find Airframe HTTP interfaces and build a Router object
    * @param targetPackages
    * @param classLoader
    */
  def buildRouter(targetPackages: Seq[String], classLoader: URLClassLoader): Router = {
    trace(s"buildRouter: ${targetPackages}\n${classLoader.getURLs.mkString("\n")}")

    // We need to use our own class loader as sbt's layered classloader cannot find application classes
    val currentClassLoader = Thread.currentThread().getContextClassLoader
    try {
      Thread.currentThread().setContextClassLoader(classLoader)

      val lst     = HttpInterfaceScanner.scanClasses(classLoader, targetPackages)
      val classes = Seq.newBuilder[Class[_]]
      lst.foreach { x =>
        Try(classLoader.loadClass(x)) match {
          case Success(cl) => classes += cl
          case _           =>
        }
      }
      buildRouter(classes.result())
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader)
    }
  }

  def buildRouter(classes: Seq[Class[_]]): Router = {
    var router = Router.empty
    for (cl <- classes) yield {
      debug(f"Searching ${cl} for HTTP endpoints")
      import wvlet.airframe.surface.reflect._
      val s       = ReflectSurfaceFactory.ofClass(cl)
      val methods = ReflectSurfaceFactory.methodsOfClass(cl)
      if (methods.exists(_.findAnnotationOf[Endpoint].isDefined)) {
        info(s"Found an Airframe HTTP interface: ${s.fullName}")
        router = router.addInternal(s, methods)
      }
    }
    router
  }

}
