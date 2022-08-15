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
package wvlet.airframe.sql.catalog

import wvlet.airframe.sql.catalog.DataType.TypeVariable
import wvlet.airspec.AirSpec
import wvlet.log.io.{IOUtil, Resource}

class DataTypeParserTest extends AirSpec {
  test("parse various types") {
    val types = IOUtil.readAsString(Resource.find("wvlet.airframe.sql.catalog", "types.txt").get).split("\n")
    for (t <- types) {
      val dt = DataTypeParser.parse(t)
      debug(s"parse: ${t} -> ${dt}")
    }
  }

  test("parse type args") {
    val args = IOUtil.readAsString(Resource.find("wvlet.airframe.sql.catalog", "argtypes.txt").get).split("\n")
    for (a <- args) {
      val dt = DataTypeParser.parseTypeList(a)
      debug(s"parse type args: ${a} -> ${dt}")
    }
  }

  test("parse type variable") {
    val t = DataTypeParser.parse("V")
    t shouldBe TypeVariable("V")
  }
}
