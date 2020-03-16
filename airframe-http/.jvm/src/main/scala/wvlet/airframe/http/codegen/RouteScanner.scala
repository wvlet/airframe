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
package wvlet.airframe.http.codegen
import java.net.URLClassLoader

import wvlet.airframe.http.{Endpoint, Router}
import wvlet.log.LogSupport

import scala.util.{Success, Try}

/**
  * Scans Airframe HTTP interfaces from the class loader and build a Router object
  */
object RouteScanner extends LogSupport {

  /**
    * Run the code block by using a given class loader
    * @param cl
    * @param body
    * @tparam U
    * @return
    */
  private def withClassLoader[U](cl: ClassLoader)(body: => U): U = {
    val prevCl = Thread.currentThread().getContextClassLoader
    try {
      Thread.currentThread().setContextClassLoader(cl)
      body
    } finally {
      Thread.currentThread().setContextClassLoader(prevCl)
    }
  }

  /**
    * Find Airframe HTTP interfaces and build a Router object
    * @param targetPackages
    * @param classLoader
    */
  def buildRouter(targetPackages: Seq[String], classLoader: URLClassLoader): Router = {
    trace(s"buildRouter: ${targetPackages}\n${classLoader.getURLs.mkString("\n")}")

    // We need to use our own class loader as sbt's layered classloader cannot find application classes
    val currentClassLoader = Thread.currentThread().getContextClassLoader
    withClassLoader(classLoader) {
      val lst     = ClassScanner.scanClasses(classLoader, targetPackages)
      val classes = Seq.newBuilder[Class[_]]
      lst.foreach { x =>
        Try(classLoader.loadClass(x)) match {
          case Success(cl) => classes += cl
          case _           =>
        }
      }
      buildRouter(classes.result())
    }
  }

  private[codegen] def buildRouter(classes: Seq[Class[_]]): Router = {
    var router = Router.empty
    for (cl <- classes) yield {
      debug(f"Searching ${cl} for HTTP endpoints")
      import wvlet.airframe.surface.reflect._
      val s       = ReflectSurfaceFactory.ofClass(cl)
      val methods = ReflectSurfaceFactory.methodsOfClass(cl)
      if (methods.exists(_.findAnnotationOf[Endpoint].isDefined)) {
        debug(s"Found an Airframe HTTP interface: ${s.fullName}")
        router = router.addInternal(s, methods)
      }
    }
    router
  }

}
