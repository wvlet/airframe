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

  test("codec examples") {
    runAll("codec")
  }

  test("di examples") {
    runAll("di")
  }

  test("control examples") {
    runAll("control")
  }

  test("http examples") {
    runAll("http")
  }

  test("launcher examples") {
    runAll("launcher")
  }

  test("log examples") {
    runAll("log")
  }

  test("surface examples") {
    runAll("surface")
  }

  test("json examples") {
    runAll("json")
  }
}
