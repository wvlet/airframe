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

import java.lang.reflect.InvocationTargetException

import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.Logger

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import wvlet.airframe.surface.reflect.{ReflectSurfaceFactory, ReflectTypeUtil}
import wvlet.airspec.spi.AirSpecException

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
  *
  */
private[airspec] object Compat extends CompatApi {
  override def isScalaJs = false

  private[airspec] def await[A](f: Future[A]): A = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Await.result(f, Duration.Inf)
  }

  private[airspec] def findCompanionObjectOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    val cls = classLoader.loadClass(fullyQualifiedName)
    ReflectTypeUtil.companionObject(cls)
  }

  private[airspec] def newInstanceOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    Try(classLoader.loadClass(fullyQualifiedName).getDeclaredConstructor().newInstance()) match {
      case Success(x) => Some(x)
      case Failure(e: InvocationTargetException)
          if classOf[spi.AirSpecException].isAssignableFrom(e.getCause.getClass) =>
        throw e
      case _ => None
    }
  }

  private[airspec] def withLogScanner[U](block: => U): U = {
    Logger.setDefaultFormatter(SourceCodeLogFormatter)

    // Periodically scan log level file
    Logger.scheduleLogLevelScan
    try {
      block
    } finally {
      Logger.stopScheduledLogLevelScan
    }
  }

  @tailrec private[airspec] def findCause(e: Throwable): Throwable = {
    e match {
      case i: InvocationTargetException => findCause(i.getTargetException)
      case _                            => e
    }
  }
  override private[airspec] def methodSurfacesOf(cls: Class[_]) = {
    ReflectSurfaceFactory.methodsOfClass(cls)
  }

  override private[airspec] def getSpecName(cl: Class[_]): String = {
    var name = cl.getName

    if (name.endsWith("$")) {
      // Remove trailing $ of Scala Object name
      name = name.substring(0, name.length - 1)
    }

    // When class is an anonymous trait
    if (name.contains("$anon$")) {
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
