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
import wvlet.airframe.stream.sql.parser.SQLParser.anonymizeSQL

import scala.collection.JavaConverters._

/**
  *
  */
class SQLTokenizerTest extends AirframeSpec {
  "tokenize SQL" in {
    anonymizeSQL("select a, b, c from t where time <= 1000")
    anonymizeSQL("select a, b, c from t where c = 'leo' and td_interval(time, '-1d')")
  }
}
