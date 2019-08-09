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
package wvlet.airframe.spec

import wvlet.airframe.Design
import wvlet.log.LogSupport

/**
  *
  */
class TestJSSpec extends AirSpec with LogSupport {
  scalaJsSupport

  override protected def configure(design: Design): Design = {
    design
      .bind[String].toInstance("hello Scala.js")
  }

  def hello(name: String = "hello"): Unit = {
    info(name)
  }

  def `natural method name test`: Unit = {
    info("hello symbol name tests")
  }
}

class FlexWordSpecTest extends AirSpec {
  scalaJsSupport

  def `should support long method names`: Unit = {}
}
