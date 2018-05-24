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
package wvlet.airframe.msgpack.spi

/**
  *
  */
object Compat {
  def isScalaJS = true

  // Javascript has no NaN in float/double values
  // See https://github.com/scala-js/scala-js/issues/2327
  def floatToIntBits(v: Float): Int     = java.lang.Float.floatToIntBits(v)
  def doubleToLongBits(v: Double): Long = java.lang.Double.doubleToLongBits(v)
}
