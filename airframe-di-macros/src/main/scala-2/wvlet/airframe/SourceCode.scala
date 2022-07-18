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

/**
  * A hack to embed source code location where DI is used
  */
case class SourceCode(
    // Deprecated and hidden because the filePath can be too long and may contain private directory paths.
    // Removing this parameter causes binary incompatibility between AirSpec and Airframe, so preserving it here
    private val filePath: String,
    fileName: String,
    line: Int,
    col: Int
) {
  override def toString = s"${fileName}:${line}"
}

object SourceCode {
  import scala.language.experimental.macros

  implicit def generate: SourceCode = macro AirframeMacros.sourceCode
  def apply()(implicit code: SourceCode) = code
}
