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
package wvlet.airframe.sbt.http
import sbt.Keys._
import sbt._
import wvlet.airframe.surface.SurfaceFactory
import wvlet.airframe.surface.reflect.{ReflectMethodSurface, ReflectSurfaceFactory}
import wvlet.log.LogSupport
import wvlet.log.io.Resource

import scala.util.{Success, Try}

/**
  *
  */
object AirframePlugin extends AutoPlugin with LogSupport {

  trait AirframeHttpKeys {
    val airframeHttpPackages       = settingKey[Seq[String]]("The list of package names containing Airframe HTTP interfaces")
    val airframeHttpGenerateClient = taskKey[Seq[File]]("Generate the client code")

  }

  object autoImport extends AirframeHttpKeys
  import autoImport._

  override def requires: Plugins = plugins.JvmPlugin
  override def trigger           = noTrigger

  override def projectSettings = Seq(
    airframeHttpPackages := Seq(),
    airframeHttpGenerateClient := {
      wvlet.airframe.log.init
      val files    = (sources in Compile).value
      val baseDirs = (sourceDirectories in Compile).value
      def relativise(f: File): Option[File] = {
        baseDirs.collectFirst { case dir if f.relativeTo(dir).isDefined => f.relativeTo(dir).get }
      }

      val lst = for (f <- files; r <- relativise(f)) yield r
      findHttpInterface(lst)
      Seq.empty
    }
  )

  def findHttpInterface(files: Seq[File]): Unit = {
    val classLoader = getClass.getClassLoader
    val classes = files
      .map { f => f.getPath }
      .filter(_.endsWith(".scala"))
      .map(_.stripSuffix(".scala").replaceAll("/", "."))
      .map { clsName =>
        info(clsName)
        Try(Class.forName(clsName))
      }
      .collect {
        case Success(cls) =>
          info(cls)
          cls
      }

    for (cl <- classes) yield {
      val s = ReflectSurfaceFactory.ofClass(cl)
      val m = ReflectSurfaceFactory.methodsOfClass(cl)

      info(s)
      info(m)
    }

  }
}
