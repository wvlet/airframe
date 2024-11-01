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
package wvlet.airframe.log

object LoggerMacros:
  import scala.quoted.*

  inline def sourcePos(): LogSource = ${ sourcePos }

  private def sourcePos(using q: Quotes): Expr[wvlet.airframe.log.LogSource] =
    import q.reflect.*
    val pos                         = Position.ofMacroExpansion
    val line                        = Expr(pos.startLine)
    val column                      = Expr(pos.endColumn)
    val src                         = pos.sourceFile
    val srcPath: java.nio.file.Path = java.nio.file.Paths.get(src.path)
    val fileName                    = Expr(srcPath.getFileName().toString)
    '{ wvlet.airframe.log.LogSource(${ fileName }, ${ line } + 1, ${ column }) }
