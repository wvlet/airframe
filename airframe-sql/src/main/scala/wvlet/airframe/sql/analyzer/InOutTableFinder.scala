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

package wvlet.airframe.sql.analyzer
import wvlet.airframe.sql.analyzer.InOutTableFinder.TableScanContext
import wvlet.airframe.sql.analyzer.TableGraph.{Alias, SourceTable, TargetTable}
import wvlet.airframe.sql.model.LogicalPlan
import wvlet.airframe.sql.model.LogicalPlan._

/**
  * Find input/output tables in an SQL statement
  */
class InOutTableFinder {
  private var g = TableGraph.empty

  def graph = g

  def process(m: LogicalPlan, context: TableScanContext): Unit = {
    m match {
      case CreateTableAs(table, _, _, query, _) =>
        val target = TargetTable(table.sqlExpr)
        g += target
        process(query, context.withOutputTable(target))
      case InsertInto(table, _, query, _) =>
        val target = TargetTable(table.toString)
        g += target
        process(query, context.withOutputTable(target))
      case Query(withQuery, body, _) =>
        for (query <- withQuery.queries) {
          val ref = Alias(query.name.value)
          g += ref
          process(body, context.withOutputTable(ref))
        }
      case DropTable(table, _, _) =>
        val target = TargetTable(table.toString)
        g += target
      case RenameTable(from, to, _) =>
        g += Edge(SourceTable(from.toString), TargetTable(to.toString))
      case TableRef(name, _) =>
        val src = SourceTable(name.toString)
        context.target match {
          case Some(x) =>
            g += Edge(src, x)
          case None =>
            g += src
        }
      case other =>
        for (c <- m.children) {
          process(c, context)
        }
    }
  }
}

/**
  */
object InOutTableFinder {

  /**
    * A holder for remembering the target output table to connect it with the inner table inputs.
    */
  case class TableScanContext(target: Option[Node] = None) {
    def withOutputTable(node: Node) = TableScanContext(Some(node))
  }
}
