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
package wvlet.airframe.rx

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

private[airframe] case class CodeExpr[A](expr: A, sourceCode: String)

/**
  *
  */
private[airframe] object CodeExpr {
  def apply[A](expr: A): CodeExpr[A] = macro exprImpl[A]

  def exprImpl[A: c.WeakTypeTag](c: Context)(expr: c.Tree): c.Tree = {
    import c.universe._

    def treeLine(t: c.Tree): String = {
      val pos    = t.pos
      val source = pos.source

      val startLine = source.offsetToLine(pos.start)
      val endLine   = pos.line
      val lines = (startLine until endLine)
        .map { i =>
          source.lineToString(i)
        }.mkString("\n")

      println(s"${startLine}-${endLine}")
      lines.trim
    }

    val codeStr: String = treeLine(expr)
    q"new wvlet.airframe.rx.CodeExpr(${expr}, ${codeStr})"
  }
}
