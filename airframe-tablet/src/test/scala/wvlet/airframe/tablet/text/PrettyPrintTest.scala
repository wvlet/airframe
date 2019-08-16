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
package wvlet.airframe.tablet.text

/**
  *
  */
import wvlet.airframe.tablet.text.PrettyPrintTest._
import wvlet.airspec.AirSpec
class PrettyPrintTest extends AirSpec {
  def `print objects`: Unit = {
    //PrettyPrint.pp(Seq(1, 2, 3))
    PrettyPrint.pp(Seq(AP(1, "leo"), AP(2, "yui")))
    PrettyPrint.show(Seq(AP(1, "leo"), AP(2, "yui")))
  }
}

object PrettyPrintTest {
  case class AP(id: Int, name: String)
}
