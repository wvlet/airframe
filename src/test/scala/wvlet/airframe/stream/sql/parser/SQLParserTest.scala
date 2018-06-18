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
package wvlet.airframe.stream.sql.parser

import wvlet.airframe.AirframeSpec
import wvlet.airframe.stream.spi.SQL._

/**
  *
  */
class SQLParserTest extends AirframeSpec {

  def parse(sql: String): Unit = {
    SQLParser.parse(sql)
  }

  "SQLParser" should {
    "parse SQL" in {
      parse("select * from a") // Query(Seq(AllColumns(None)), false, Some(Table(QName("a"))))
      parse("select * from a where time > 10")
      parse("select * from a where time < 10")
      parse("select * from a where time < =10")
      parse("select * from a where id = 'xxxx'")
      parse("select * from a where time >= 10 and time < 20")
      parse("select * from a where id is null")
      parse("select * from a where id is not null")
      parse("select x, y from a")
    }
  }
}
