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
package wvlet.airspec

import org.portablescala.reflect.Reflect
import wvlet.airframe.surface.MethodSurface
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{ConsoleLogHandler, LogSupport, Logger}

import scala.concurrent.Future

/**
  *
  */
private[airspec] object Compat extends CompatApi with LogSupport {
  override def isScalaJs = true

  private implicit val defaultExecutionContext = scala.concurrent.ExecutionContext.global

  private[airspec] def await[A](f: Future[A]): A = {
    f.value match {
      case Some(v) => v.get
      case _ =>
        throw new IllegalStateException("Scala.js cannot support async test")
    }
  }

  private[airspec] def findCompanionObjectOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    val clsOpt = Reflect.lookupLoadableModuleClass(fullyQualifiedName + "$", classLoader)
    clsOpt.map {
      _.loadModule()
    }
  }

  private[airspec] def newInstanceOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    // Scala.js needs to use @EnableReflectiveInstantiation annotation to support .newInstance
    val clsOpt = Reflect.lookupInstantiatableClass(fullyQualifiedName)
    clsOpt.map(_.newInstance())
  }

  private[airspec] def withLogScanner[U](block: => U): U = {
    Logger.setDefaultHandler(new ConsoleLogHandler(SourceCodeLogFormatter))
    block
  }

  private[airspec] def findCause(e: Throwable): Throwable = {
    // Scala.js has no InvocationTargetException
    e
  }
  override private[airspec] def methodSurfacesOf(cls: Class[_]) = Seq.empty[MethodSurface]

  override private[airspec] def getSpecName(cl: Class[_]): String = {
    var name = cl.getName

    // In Scala.js we cannot use cl.getInterfaces to find the actual type
    val pos = name.indexOf("$")
    if (pos > 0) {
      // Remove trailing $xxx
      name = name.substring(0, pos)
    }
    name
  }
}
