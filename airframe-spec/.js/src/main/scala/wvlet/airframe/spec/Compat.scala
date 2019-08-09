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
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{ConsoleLogHandler, Logger}

/**
  *
  */
private[spec] object Compat extends CompatApi {
  private[spec] def findCompanionObjectOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    throw new IllegalStateException(s"Scala.js cannot read module classes: ${fullyQualifiedName}")
  }

  private[spec] def newInstanceOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    val clsOpt = Reflect.lookupInstantiatableClass(fullyQualifiedName)
    clsOpt.map(_.newInstance())
  }

  private[spec] def withLogScanner[U](block: => U): U = {
    Logger.setDefaultHandler(new ConsoleLogHandler(SourceCodeLogFormatter))
    block
  }

  private[spec] def findCause(e: Throwable): Throwable = {
    e
  }
  override private[spec] def methodSurfacesOf(cls: Class[_]) = {
    throw new IllegalStateException(
      s"Add this method in Scala.js: override protected def methodSurfacesOf = Surface.methodsOf[${cls.getSimpleName}]")
  }
}
