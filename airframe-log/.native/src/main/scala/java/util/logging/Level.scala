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
package java.util.logging

case class Level(name: String, value: Int) extends Ordered[Level] {
  override def compare(other: Level): Int = value.compare(other.value)
  def intValue(): Int                     = value
  override def toString: String           = name
}

object Level {
  val OFF     = Level("OFF", 0)
  val SEVERE  = Level("SEVERE", 1000)
  val WARNING = Level("WARNING", 900)
  val INFO    = Level("INFO", 800)
  val CONFIG  = Level("CONFIG", 700)
  val FINE    = Level("FINE", 500)
  val FINER   = Level("FINER", 400)
  val FINEST  = Level("FINEST", 300)
  val ALL     = Level("ALL", Integer.MIN_VALUE)
}
