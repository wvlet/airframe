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

package wvlet.surface

import org.scalatest._
import wvlet.airframe.AirframeSpec
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.language.implicitConversions

trait SurfaceSpec extends AirframeSpec {

  def check(body: => Surface, expectedName: String): Surface = {
    val surface = body
    info(s"[${surface.getClass.getSimpleName}] $surface, ${surface.fullName}")
    surface.toString shouldBe expectedName
    surface
  }

  def checkPrimitive(body: => Surface, expectedName: String): Surface = {
    val s = check(body, expectedName)
    s.isAlias shouldBe false
    s.isOption shouldBe false
    s.isPrimitive shouldBe true
    s.objectFactory.isEmpty shouldBe true
    s
  }

}
