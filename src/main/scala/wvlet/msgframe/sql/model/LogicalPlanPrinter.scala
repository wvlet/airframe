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
import java.io.PrintWriter

import wvlet.log.LogSupport

/**
  *
  */
object LogicalPlanPrinter extends LogSupport {

  def print(m: LogicalPlan, out: PrintWriter, level: Int): Unit = {
    val ws = " " * (level * 2)
    out.println(s"${ws}- ${m.modelName}")
    for (c <- m.children) {
      out.println(print(c, out, level + 1))
    }
  }
}
