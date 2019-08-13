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
package wvlet.airframe
import scala.language.experimental.macros

/**
  * Source code location.
  *
  * This code is placed under airframe-di-macros to avoid weird compilation error in IntelliJ
  * if we place this code in airframe.
  */
case class SourceCode(filePath: String, fileName: String, line: Int, col: Int) {
  override def toString = s"${fileName}:${line}"
}

object SourceCode {
  def apply()(implicit code: SourceCode) = code
  implicit def generate: SourceCode = macro AirframeMacros.sourceCode
}
