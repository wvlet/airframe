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

import sbt.Keys.{classDirectory, sourceDirectories, sources}
import sbt.{File, settingKey, taskKey, _}
import wvlet.airframe.http.router.ControllerRoute
import wvlet.airframe.http.{Endpoint, HttpRequest, Router}
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
    val airframeHttpGenerateClient = taskKey[Seq[File]]("Generate the client code")
    val airframeHttpRouter         = taskKey[Router]("Airframe Router")
  }

  def httpProjectSettings = Seq(
    airframeHttpPackages := Seq(),
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
      generateHttpClient(router)
      Seq.empty
    }
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

  def generateHttpClient(router: Router): Unit = {

    val lines = Seq.newBuilder[String]
    for ((controllerSurface, routes) <- router.routes.groupBy(_.controllerSurface)) {
      lines += s"object ${controllerSurface.name} {"
      for (r <- routes) {
        r match {
          case cr: ControllerRoute =>
            // Filter server-side only arguments
            val clientSideArgs = cr.methodSurface.args.filter { x =>
              !classOf[HttpRequest[_]].isAssignableFrom(x.surface.rawType) &&
              !x.surface.fullName.startsWith("wvlet.airframe.http.HttpContext") &&
              x.surface.fullName != "com.twitter.finagle.http.Request"
            }

            val args =
              if (clientSideArgs.isEmpty) ""
              else {
                s"(${clientSideArgs.map { x => s"${x.name}: ${x.surface.name}" }.mkString(", ")})"
              }
            val ret  = cr.returnTypeSurface.name
            val body = s"""client.${cr.method.toString.toLowerCase}("${cr.path}")"""
            lines += s"  def ${cr.methodSurface.name}${args}: ${ret} = {"
            lines += s"    ${body}"
            lines += s"  }"
          case _ =>
        }
      }
      lines += "}"
    }
    val methods = lines.result().map(x => s"  ${x}").mkString("\n")

    val code = s"""
       |package ...
       |import wvlet.airframe.http.HttpClient
       |
       |class MyHttpClient[F[_], Req, Resp](private val client:HttpClient[F, Req, Resp]) {
       |  def getClient: HttpClient[F, Req, Resp] = client
       |${methods}
       |}
       |""".stripMargin

    info(code)

  }

}
