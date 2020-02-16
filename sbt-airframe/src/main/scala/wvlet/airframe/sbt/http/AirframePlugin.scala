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
import wvlet.log.LogSupport
import wvlet.log.io.Resource

import scala.util.Try

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
      for (p <- airframeHttpPackages.value) yield {
        findHttpInterface(p)
      }
      Seq.empty
    }
  )

  def findHttpInterface(packageName: String): Unit = {
    wvlet.airframe.log.init
    info(s"Searching ${packageName}")

    Resource
      .listResources(packageName, _.endsWith(".class"), getClass.getClassLoader)
      .map(_.logicalPath.stripSuffix(".class").replaceAll("/", "."))
      .map(path => info(path))
  }
}
