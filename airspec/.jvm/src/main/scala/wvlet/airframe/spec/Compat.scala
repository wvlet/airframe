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

import java.lang.reflect.InvocationTargetException

import wvlet.airframe.surface.reflect.{ReflectSurfaceFactory, ReflectTypeUtil}
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.Logger

import scala.annotation.tailrec
import scala.util.Try

/**
  *
  */
private[spec] object Compat extends CompatApi {
  override def isScalaJs = false

  private[spec] def findCompanionObjectOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    val cls = classLoader.loadClass(fullyQualifiedName)
    ReflectTypeUtil.companionObject(cls)
  }

  private[spec] def newInstanceOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    Try(classLoader.loadClass(fullyQualifiedName).getDeclaredConstructor().newInstance()).toOption
  }

  private[spec] def withLogScanner[U](block: => U): U = {
    Logger.setDefaultFormatter(SourceCodeLogFormatter)

    // Periodically scan log level file
    Logger.scheduleLogLevelScan
    try {
      block
    } finally {
      Logger.stopScheduledLogLevelScan
    }
  }

  @tailrec private[spec] def findCause(e: Throwable): Throwable = {
    e match {
      case i: InvocationTargetException => findCause(i.getTargetException)
      case _                            => e
    }
  }
  override private[spec] def methodSurfacesOf(cls: Class[_]) = {
    ReflectSurfaceFactory.methodsOfClass(cls)
  }

  override private[spec] def getSpecName(cl: Class[_]): String = {
    var name = cl.getName

    if (name.endsWith("$")) {
      // Remove trailing $ of Scala Object name
      name = name.substring(0, name.length - 1)
    }

    // When class is an anonymous trait
    if (name.contains("$anon$")) {
      import collection.JavaConverters._
      val interfaces = cl.getInterfaces
      if (interfaces != null && interfaces.length > 0) {
        // Use the first interface name instead of the anonymous name and Airframe SessionHolder (injected at compile-time)
        interfaces
          .map(_.getName)
          .find(x => x != "wvlet.airframe.SessionHolder" && !x.contains("$anon$"))
          .foreach(name = _)
      }
    }
    name
  }
}
