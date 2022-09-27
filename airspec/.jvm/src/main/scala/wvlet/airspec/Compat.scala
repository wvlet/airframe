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
import sbt.testing.Fingerprint
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogSupport, Logger}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import wvlet.airframe.surface.reflect.ReflectTypeUtil
import wvlet.airspec.Framework.{AirSpecClassFingerPrint, AirSpecObjectFingerPrint}
import wvlet.airspec.spi.{AirSpecException, Asserts}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

/**
  */
private[airspec] object Compat extends CompatApi with LogSupport {
  override def isScalaJs = false

  override private[airspec] val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  private[airspec] def findCompanionObjectOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    val cls = classLoader.loadClass(fullyQualifiedName)
    ReflectTypeUtil.companionObject(cls)
  }

  private[airspec] def getFingerprint(fullyQualifiedName: String, classLoader: ClassLoader): Option[Fingerprint] = {
    Try(findCompanionObjectOf(fullyQualifiedName, classLoader)).toOption
      .flatMap {
        case Some(spec: AirSpecSpi) =>
          Some(AirSpecObjectFingerPrint)
        case other =>
          None
      }
      .orElse {
        Try(classLoader.loadClass(fullyQualifiedName)).toOption
          .flatMap { x =>
            if (classOf[AirSpec].isAssignableFrom(x))
              Some(AirSpecClassFingerPrint)
            else {
              None
            }
          }
      }
  }

  private[airspec] def newInstanceOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    Try(classLoader.loadClass(fullyQualifiedName).getDeclaredConstructor().newInstance()) match {
      case Success(x) => Some(x)
      case Failure(e: InvocationTargetException) if e.getCause != null =>
        if (classOf[spi.AirSpecException].isAssignableFrom(e.getCause.getClass)) {
          // For assertion failrues, throw it as is
          throw e
        } else {
          // For other failures when instantiating the object, throw the cause
          throw e.getCause
        }
      case _ =>
        // Ignore other types of failures, which should not happen in general
        None
    }
  }

  private[airspec] def withLogScanner[U](block: => U): U = {
    try {
      startLogScanner
      block
    } finally {
      stopLogScanner
    }
  }

  private[airspec] def startLogScanner: Unit = {
    Logger.setDefaultFormatter(SourceCodeLogFormatter)

    // Periodically scan log level file
    Logger.scheduleLogLevelScan
  }
  private[airspec] def stopLogScanner: Unit = {
    Logger.stopScheduledLogLevelScan
  }

  @tailrec private[airspec] def findCause(e: Throwable): Throwable = {
    e match {
      case i: InvocationTargetException => findCause(i.getTargetException)
      case _                            => e
    }
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

  private[airspec] def getContextClassLoader: ClassLoader = {
    Thread.currentThread().getContextClassLoader
  }
}
