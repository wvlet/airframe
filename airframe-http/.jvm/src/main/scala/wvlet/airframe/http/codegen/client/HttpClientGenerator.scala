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
package wvlet.airframe.http.codegen.client
import wvlet.airframe.http.codegen.HttpClientIR.ClientSourceDef

/**
  */
trait HttpClientGenerator {
  def name: String
  def defaultFileName: String
  def defaultClassName: String
  def generate(src: ClientSourceDef): String
}

object HttpClientGenerator {

  def predefinedClients: Seq[HttpClientGenerator] =
    Seq(
      AsyncClientGenerator,
      SyncClientGenerator,
      ScalaJSClientGenerator,
      GrpcClientGenerator
    )

  def findClient(name: String): Option[HttpClientGenerator] = {
    predefinedClients.find(_.name == name)
  }
}
