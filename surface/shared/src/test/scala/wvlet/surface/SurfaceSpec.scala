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
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.language.implicitConversions

trait SurfaceSpec extends WordSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with LogSupport {

  // Add source code location to the debug logs
  Logger.setDefaultFormatter(SourceCodeLogFormatter)

  implicit def toTag(s:String) = Tag(s)

  override def run(testName: Option[String], args: Args): Status = {
    // Periodically scan log level file
    Logger.scheduleLogLevelScan
    val s = super.run(testName, args)
    Logger.stopScheduledLogLevelScan
    s
  }

  def check(body: => Surface, expectedName:String): Surface = {
    val surface = body
    info(s"[${surface.getClass.getSimpleName}] $surface, ${surface.fullName}")
    surface.toString shouldBe expectedName
    surface
  }

  def checkPrimitive(body: => Surface, expectedName:String) : Surface = {
    val s = check(body, expectedName)
    s.isAlias shouldBe false
    s.isOption shouldBe false
    s.isPrimitive shouldBe true
    s.objectFactory.isEmpty shouldBe true
    s
  }

}
