/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LIkCENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.examples

import wvlet.airframe.surface.reflect.ReflectTypeUtil
import wvlet.airspec.AirSpec
import wvlet.log.io.Resource

/**
  */
class ExamplesSpec extends AirSpec {
  private def runAll(packageName: String): Unit = {
    for {
      cl  <- Resource.findClasses(s"wvlet.airframe.examples.${packageName}", classOf[App]).sortBy(_.getSimpleName)
      app <- ReflectTypeUtil.companionObject(cl)
    } {
      warn(s"Running ${app.getClass.getSimpleName.replaceAll("\\$", "")}")

      app.asInstanceOf[App].main(Array.empty)
    }
  }

  def `codec examples`: Unit = {
    runAll("codec")
  }

  def `di examples`: Unit = {
    runAll("di")
  }

  def `control examples`: Unit = {
    runAll("control")
  }

  def `http examples`: Unit = {
    runAll("http")
  }

  def `launcher examples`: Unit = {
    runAll("launcher")
  }

  def `log examples`: Unit = {
    runAll("log")
  }

  def `surface examples`: Unit = {
    runAll("surface")
  }

  def `json examples`: Unit = {
    runAll("json")
  }
}
