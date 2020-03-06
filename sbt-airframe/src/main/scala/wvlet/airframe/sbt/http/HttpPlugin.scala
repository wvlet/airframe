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
import sbt.{File, settingKey, taskKey, _}
import wvlet.airframe.http.{Endpoint, Router}
import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

/**
  *
  */
object HttpPlugin extends LogSupport {
  wvlet.airframe.log.init

  trait AirframeHttpKeys {
    val airframeHttpPackages       = settingKey[Seq[String]]("A list of package names containing Airframe HTTP interfaces")
    val airframeHttpGenerateClient = taskKey[Seq[File]]("Generate the client code")
    val airframeHttpRouter         = taskKey[Router]("Airframe Router")
  }

  case class HttpInterface(surface: Surface, endpoints: Seq[HttpEndpoint])
  case class HttpEndpoint(endpoint: Endpoint, method: MethodSurface)

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

    var router = Router.empty
    for (cl <- classes) yield {
      import wvlet.airframe.surface.reflect._
      val s       = ReflectSurfaceFactory.ofClass(cl)
      val methods = ReflectSurfaceFactory.methodsOfClass(cl)
      if (methods.exists(_.findAnnotationOf[Endpoint].isDefined)) {
        info(s"Adding ${s.fullName} to Router")
        router = router.addInternal(s, methods)
      }
    }
    router
  }

}
