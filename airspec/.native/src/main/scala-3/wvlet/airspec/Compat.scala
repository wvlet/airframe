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

import sbt.testing.Fingerprint
import wvlet.airframe.surface.MethodSurface
import wvlet.airspec.Framework.{AirSpecClassFingerPrint, AirSpecObjectFingerPrint}
import wvlet.airspec.spi.Asserts
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{ConsoleLogHandler, LogSupport, Logger}

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.{Executors, ThreadFactory}
import scala.concurrent.ExecutionContext

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.{Success, Failure}

/**
  */
private[airspec] object Compat extends CompatApi with LogSupport:
  override def isScalaJs = false

  override private[airspec] val executionContext: ExecutionContext = ExecutionContext.global

  private[airspec] def findCompanionObjectOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] =
    val clsOpt = scala.scalanative.reflect.Reflect.lookupLoadableModuleClass(fullyQualifiedName + "$")
    clsOpt.map {
      _.loadModule()
    }

  private[airspec] def getFingerprint(fullyQualifiedName: String, classLoader: ClassLoader): Option[Fingerprint] =
    println(s"Checking class fingerprint for ${fullyQualifiedName}")
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
            if classOf[AirSpec].isAssignableFrom(x) then Some(AirSpecClassFingerPrint)
            else None
          }
      }

  private[airspec] def newInstanceOf(fullyQualifiedName: String, classLoader: ClassLoader): Option[Any] = {
    val clsOpt = scala.scalanative.reflect.Reflect.lookupInstantiatableClass(fullyQualifiedName)
    clsOpt.map(_.newInstance())
  }

  private[airspec] def withLogScanner[U](block: => U): U =
    try
      startLogScanner
      block
    finally stopLogScanner

  private[airspec] def startLogScanner: Unit = {}
  private[airspec] def stopLogScanner: Unit  = {}

  private[airspec] def findCause(e: Throwable): Throwable =
    e match
      case i: InvocationTargetException => findCause(i.getTargetException)
      case _                            => e

  override private[airspec] def getSpecName(cl: Class[?]): String =
    var name = cl.getName

    // In Scala.js we cannot use cl.getInterfaces to find the actual type
    val pos = name.indexOf("$")
    if pos > 0 then
      // Remove trailing $xxx
      name = name.substring(0, pos)
    name

  private[airspec] def getContextClassLoader: ClassLoader =
    // Scala.js doesn't need to use ClassLoader for loading tests
    null
