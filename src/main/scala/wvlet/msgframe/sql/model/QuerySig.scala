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
import wvlet.log.LogSupport
import wvlet.msgframe.sql.model.QueryGraph._
import wvlet.msgframe.sql.model.SQLModel._
import wvlet.msgframe.sql.parser.SQLParser

/**
  *
  */
object QuerySig extends LogSupport {

  def findInputOutputTableGraph(sql: String) = {
    val m      = SQLParser.parse(sql)
    val finder = new TableInOutFinder
    finder.process(m, TableScanContext(Some(Terminal)))

    finder.graph
  }

  def sig(sql: String): String = {
    SQLParser.parse(sql) match {
      case m: SQLSig => m.sig
      case other     => "Unknown"
    }
  }

  case class TableScanContext(target: Option[QueryGraph.Node] = None) {
    def withOutput(node: QueryGraph.Node) = TableScanContext(Some(node))
  }

  class TableInOutFinder {
    private var g = QueryGraph.emptyGraph

    def graph = g

    def process(m: SQLModel, context: TableScanContext): Unit = {
      m match {
        case CreateTableAs(table, _, _, query) =>
          val target = TargetTable(table.toString)
          g += target
          process(query, context.withOutput(target))
        case InsertInto(table, _, query) =>
          val target = TargetTable(table.toString)
          g += target
          process(query, context.withOutput(target))
        case Query(withQuery, body) =>
          for (query <- withQuery.queries) {
            val ref = Alias(query.name.toString)
            g += ref
            process(body, context.withOutput(ref))
          }
        case DropTable(table, _) =>
          val target = TargetTable(table.toString)
          g += target
        case Table(name) =>
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

}
