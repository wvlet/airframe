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
import wvlet.airframe.http.*
import wvlet.airframe.surface.reflect.ReflectTypeUtil
import wvlet.airframe.surface.{Surface, TypeName}
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

  private[codegen] def buildRxRouter(targetPackages: Seq[String]): RxRouter = {
    buildRxRouter(targetPackages, Thread.currentThread().getContextClassLoader)
  }

  def buildRxRouter(targetPackages: Seq[String], classLoader: ClassLoader): RxRouter = {
    // We need to use our own class loader as sbt's layered classloader cannot find application classes
    withClassLoader(classLoader) {
      val lst = ClassScanner.scanClasses(classLoader, targetPackages)
      trace(s"classes: ${lst.mkString(", ")}")
      val rxRouterProviderClasses = Seq.newBuilder[Class[RxRouterProvider]]
      lst.foreach { x =>
        Try(classLoader.loadClass(x)) match {
          case Success(cl) if classOf[RxRouterProvider].isAssignableFrom(cl) =>
            rxRouterProviderClasses += cl.asInstanceOf[Class[RxRouterProvider]]
          case _ =>
        }
      }

      val routers = rxRouterProviderClasses
        .result()
        .map { cl => ReflectTypeUtil.companionObject(cl) }
        .collect { case Some(obj) => obj }
        .collect { case rxRouterProvider: RxRouterProvider =>
          debug(s"Found an RxRouterProvider: ${TypeName.sanitizeTypeName(rxRouterProvider.getClass.getName)}")
          rxRouterProvider.router
        }

      if (routers.isEmpty) {
        error(
          s"No router definition is found. Make sure implementing RxRouterProvider in your api objects"
        )
      }
      RxRouter.of(routers: _*)
    }
  }

  /**
    * Find Airframe HTTP interfaces and build a Router object
    * @param targetPackages
    * @param classLoader
    */
  @deprecated("Use buildRxRouter instead", since = "23.5.0")
  def buildRouter(targetPackages: Seq[String], classLoader: ClassLoader): Router = {
    throw new UnsupportedOperationException("Use buildRxRouter instead")
  }

  @deprecated("Use buildRxRouter instead", since = "23.5.0")
  private[codegen] def buildRouter(classes: Seq[Class[_]]): Router = {
    throw new UnsupportedOperationException("Use buildRxRouter instead")
  }

}
