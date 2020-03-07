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
import sbt.{File, settingKey, taskKey, _}
import wvlet.airframe.http.{Endpoint, Router}
import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

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

  trait AirframeHttpKeys {
    val airframeHttpPackages       = settingKey[Seq[String]]("A list of package names containing Airframe HTTP interfaces")
    val airframeHttpTargetPackage  = settingKey[Option[String]]("Generate target package")
    val airframeHttpGenerateClient = taskKey[Seq[File]]("Generate the client code")
    val airframeHttpRouter         = taskKey[Router]("Airframe Router")
  }

  def httpProjectSettings =
    Seq(
      airframeHttpPackages := Seq(),
      airframeHttpTargetPackage := None,
      airframeHttpRouter := {
        val files       = (sources in Compile).value
        val baseDirs    = (sourceDirectories in Compile).value
        val classDir    = (classDirectory in Runtime).value
        val classLoader = new URLClassLoader(Array(classDir.toURI.toURL), getClass.getClassLoader)
        val router      = buildRouter(baseDirs, files, classLoader)
        info(router)
        router
      },
      airframeHttpGenerateClient := {
        val router = airframeHttpRouter.value
        HttpClientGenerator.generateHttpClient(router, airframeHttpTargetPackage.value)
        Seq.empty
      },
      Compile / sourceGenerators += Def.task {
        val path       = airframeHttpTargetPackage.value.getOrElse("generated").replaceAll("\\.", "/")
        val file: File = (Compile / sourceManaged).value / path / "ServiceClient.scala"
        Seq(file)
      }.taskValue
    )

  /**
    * Find Airframe HTTP interfaces and build a Router object
    * @param sourceDirs
    * @param files
    * @param classLoader
    */
  def buildRouter(sourceDirs: Seq[File], files: Seq[File], classLoader: ClassLoader): Router = {
    def relativise(f: File): Option[File] = {
      sourceDirs.collectFirst { case dir if f.relativeTo(dir).isDefined => f.relativeTo(dir).get }
    }
    val lst = for (f <- files; r <- relativise(f)) yield r

    val classes = lst
      .map { f => f.getPath }
      .filter(_.endsWith(".scala"))
      .map(_.stripSuffix(".scala").replaceAll("/", "."))
      .map { clsName =>
        trace(s"Searching endpoints in ${clsName}")
        Try(classLoader.loadClass(clsName)) match {
          case x if x.isSuccess => x
          case f @ Failure(e) =>
            f
        }
      }
      .collect {
        case Success(cls) =>
          cls
      }

    buildRouter(classes)
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
