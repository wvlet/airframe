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

/**
  *
  */
object AirframePlugin extends AutoPlugin {

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
      val logger = state.value.log
      for (p <- airframeHttpPackages.value) yield {
        logger.info(s"processing ${p}")
      }
      Seq.empty
    }
  )
}
