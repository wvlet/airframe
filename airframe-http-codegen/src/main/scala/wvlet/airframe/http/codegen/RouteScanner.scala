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
import wvlet.airframe.http.router.{RxRouter, RxRouterProvider}
import wvlet.airframe.http.{Endpoint, RPC, Router}
import wvlet.airframe.surface.TypeName
import wvlet.airframe.surface.reflect.ReflectTypeUtil
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
    trace(s"buildRouter: ${targetPackages}")

    // We need to use our own class loader as sbt's layered classloader cannot find application classes
    withClassLoader(classLoader) {
      val lst = ClassScanner.scanClasses(classLoader, targetPackages)
      trace(s"classes: ${lst.mkString(", ")}")
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

  @deprecated("Use buildRxRouter instead", since = "23.5.0")
  private[codegen] def buildRouter(classes: Seq[Class[_]]): Router = {
    var router = Router.empty
    // Find classes with @RPC or @Endpoint annotations.
    //
    // Note: We need to remove object classes ending with $, because Surface.fullTypeNameOf(...)
    // will not distinguish regular classes and their corresponding objects.
    // This is because we generally cannot call classOf[MyObj$] in Scala other than scanning classes directly from class loaders.
    for (cl <- classes if !cl.getName.endsWith("$")) {
      trace(f"Searching ${cl} for HTTP endpoints")
      import wvlet.airframe.surface.reflect._
      lazy val s       = ReflectSurfaceFactory.ofClass(cl)
      lazy val methods = ReflectSurfaceFactory.methodsOfClass(cl)
      val hasRPC       = findDeclaredAnnotation[RPC](cl).isDefined
      if (hasRPC) {
        debug(s"Found an Airframe RPC interface: ${s.fullName}")
        router = router.addInternal(s, methods)
      } else if (methods.exists(m => m.findAnnotationOf[Endpoint].isDefined)) {
        debug(s"Found an Airframe HTTP interface: ${s.fullName}")
        router = router.addInternal(s, methods)
      }
    }
    // Check whether the route is valid or not
    router.verifyRoutes
    router
  }

}
