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
class AirframeHttpPlugin extends AutoPlugin {

  trait AirframeHttpKeys {
    val airframeHttpPackages       = settingKey[Seq[String]]("The list of package names containing Airframe HTTP interfaces")
    val airframeHttpGenerateClient = taskKey[Seq[File]]("Generate the client code")

  }

  object AirframeHttpKeys extends AirframeHttpKeys
  import AirframeHttpKeys._

  override def trigger = allRequirements

  override def projectSettings = Seq(
    airframeHttpGenerateClient := {
      val logger = state.value.log

      for (p <- airframeHttpPackages.value) yield {
        logger.info(s"processing ${p}")
      }
      Seq.empty
    }
  )

  import sbt.ScriptedPlugin.autoImport._
  import complete.DefaultParsers._
}
