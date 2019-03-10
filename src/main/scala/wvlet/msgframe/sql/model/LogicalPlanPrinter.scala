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

package wvlet.msgframe.sql.model
import java.io.{PrintWriter, StringWriter}

import wvlet.log.LogSupport
import wvlet.msgframe.sql.model.LogicalPlan.EmptyRelation

/**
  *
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
      case EmptyRelation =>
      // print nothing
      case _ =>
        val ws   = " " * (level)
        val attr = m.expressions.map(_.toString)
        attr.length match {
          case 0 =>
            out.println(s"${ws}[${m.modelName}]")
          case 1 =>
            out.println(s"${ws}[${m.modelName}] ${attr.mkString(", ")}")
          case _ =>
            out.println(s"${ws}[${m.modelName}]")
            val attrWs  = " " * (level + 1)
            val attrStr = attr.map(x => s"${attrWs}- ${x}").mkString("\n")
            out.println(attrStr)
        }
        for (c <- m.children) {
          print(c, out, level + 1)
        }
    }
  }
}
