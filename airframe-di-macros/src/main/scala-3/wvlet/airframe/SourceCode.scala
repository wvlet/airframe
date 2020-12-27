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
case class SourceCode(filePath: String, fileName: String, line: Int, col: Int) {
  override def toString = s"${fileName}:${line}"
}

object SourceCode {
  def apply()(implicit code: SourceCode) = code

  import scala.quoted._

  inline implicit def generate: SourceCode = ${ generateImpl }

  def generateImpl(using q: Quotes): Expr[SourceCode] = {
    import q.reflect._
    val pos = Position.ofMacroExpansion
    val line = Expr(pos.startLine)
    val column = Expr(pos.endColumn)
    val src = pos.sourceFile
    val srcPath: java.nio.file.Path = src.jpath
    val path = Expr(srcPath.toFile.getPath)
    val fileName = Expr(srcPath.getFileName().toString)
    '{ SourceCode(${path}, ${fileName}, ${line} + 1, ${column}) }
  }
}
