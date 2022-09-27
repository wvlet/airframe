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
import sbt.testing.Fingerprint
import wvlet.airframe.surface.MethodSurface
import wvlet.airspec.Framework.{AirSpecClassFingerPrint, AirSpecObjectFingerPrint}
import wvlet.airspec.spi.{Asserts, JsObjectMatcher}
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{ConsoleLogHandler, LogSupport, Logger}

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  */
private[airspec] object Compat extends CompatApi with LogSupport {
  override def isScalaJs = true

  override private[airspec] val executionContext: ExecutionContext =
    org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

  private[airspec] def findCompanionObjectOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    val clsOpt = Reflect.lookupLoadableModuleClass(fullyQualifiedName + "$", classLoader)
    clsOpt.map {
      _.loadModule()
    }
  }

  private[airspec] def getFingerprint(fullyQualifiedName: String, classLoader: ClassLoader): Option[Fingerprint] = {
    Try(findCompanionObjectOf(fullyQualifiedName, classLoader)).toOption
      .flatMap { x =>
        x match {
          case Some(spec: AirSpecSpi) => Some(AirSpecObjectFingerPrint)
          case _                      => None
        }
      }
      .orElse {
        Reflect
          .lookupInstantiatableClass(fullyQualifiedName)
          .flatMap { x =>
            if (classOf[AirSpecSpi].isAssignableFrom(x.runtimeClass)) {
              Some(AirSpecClassFingerPrint)
            } else {
              None
            }
          }
      }
  }

  private[airspec] def newInstanceOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    // Scala.js needs to use @EnableReflectiveInstantiation annotation to support .newInstance
    val clsOpt = Reflect.lookupInstantiatableClass(fullyQualifiedName)
    clsOpt.map(_.newInstance())
  }

  private[airspec] def withLogScanner[U](block: => U): U = {
    startLogScanner
    block
  }

  private[airspec] def startLogScanner: Unit = {
    Logger.setDefaultHandler(new ConsoleLogHandler(SourceCodeLogFormatter))
  }
  private[airspec] def stopLogScanner: Unit = {}

  private[airspec] def findCause(e: Throwable): Throwable = {
    // Scala.js has no InvocationTargetException
    e
  }

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

  private[airspec] def getContextClassLoader: ClassLoader = {
    // Scala.js doesn't need to use ClassLoader for loading tests
    null
  }

  override private[airspec] def platformSpecificMatcher: PartialFunction[(Any, Any), Asserts.TestResult] =
    JsObjectMatcher.matcher

  import scala.scalajs.js
  override private[airspec] def platformSpecificPrinter: PartialFunction[Any, String] = { case x: js.Object =>
    Try(js.JSON.stringify(x)).getOrElse(x.toLocaleString())
  }
}
