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
package wvlet.airframe.spec

import org.portablescala.reflect._
import wvlet.airframe.surface.MethodSurface
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{ConsoleLogHandler, LogSupport, Logger}

/**
  *
  */
private[spec] object Compat extends CompatApi with LogSupport {
  override def isScalaJs = true
  private[spec] def findCompanionObjectOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    val clsOpt = Reflect.lookupLoadableModuleClass(fullyQualifiedName + "$", classLoader)
    clsOpt.map {
      _.loadModule()
    }
  }

  private[spec] def newInstanceOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    // Scala.js needs to use @EnableReflectiveInstantiation annotation to support .newInstance
    val clsOpt = Reflect.lookupInstantiatableClass(fullyQualifiedName)
    clsOpt.map(_.newInstance())
  }

  private[spec] def withLogScanner[U](block: => U): U = {
    Logger.setDefaultHandler(new ConsoleLogHandler(SourceCodeLogFormatter))
    block
  }

  private[spec] def findCause(e: Throwable): Throwable = {
    // Scala.js has no InvocationTargetException
    e
  }
  override private[spec] def methodSurfacesOf(cls: Class[_]) = Seq.empty[MethodSurface]

}
