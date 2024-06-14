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

package wvlet.airframe.sql.model
import wvlet.airframe.sql.catalog.DataType.RecordType
import wvlet.airframe.sql.model.Expression.AllColumns

import java.io.{PrintWriter, StringWriter}
import wvlet.log.LogSupport
import wvlet.airframe.sql.model.LogicalPlan.EmptyRelation

/**
  */
object LogicalPlanPrinter extends LogSupport {
  def print(m: LogicalPlan): String = {
    val s = new StringWriter()
    val p = new PrintWriter(s)
    print(m, p, 0)
    p.close()
    s.toString
  }

  def print(m: LogicalPlan, out: PrintWriter, level: Int): Unit = {
    m match {
      case EmptyRelation(_) =>
      // print nothing
      case _ =>
        val ws = "  " * level

        val inputAttrs  = m.inputAttributes
        val outputAttrs = m.outputAttributes
        val attr        = m.childExpressions.map(_.toString)
        val functionSig =
          if inputAttrs.isEmpty && outputAttrs.isEmpty then {
            ""
          } else {
            def printAttr(s: Seq[Attribute]): String = {
              val lst = s
                .map(_.typeDescription)
                .mkString(", ")
              if s.size >= 1 then {
                s"(${lst})"
              } else {
                lst
              }
            }
            s": ${printAttr(inputAttrs)} => ${printAttr(outputAttrs)}"
          }

        val prefix = m match {
          case t: TableScan =>
            s"${ws}[${m.modelName}] ${t.table.fullName}${functionSig}"
          case _ =>
            s"${ws}[${m.modelName}]${functionSig}"
        }

        attr.length match {
          case 0 =>
            out.println(prefix)
          case _ =>
            out.println(s"${prefix}")
            val attrWs  = "  " * (level + 1)
            val attrStr = attr.map(x => s"${attrWs}- ${x}").mkString("\n")
            out.println(attrStr)
        }
        for c <- m.children do {
          print(c, out, level + 1)
        }
    }
  }
}
