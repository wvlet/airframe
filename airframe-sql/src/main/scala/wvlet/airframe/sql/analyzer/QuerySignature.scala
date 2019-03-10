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
import wvlet.log.LogSupport
import wvlet.airframe.sql.model.SQLSig
import wvlet.airframe.sql.parser.SQLParser

/**
  *
  */
object QuerySignature extends LogSupport {

  def of(sql: String): String = {
    val plan  = SQLParser.parse(sql)
    val g     = TableGraph.of(plan)
    val inout = printEdges(g)
    plan match {
      case s: SQLSig =>
        s"${s.sig} ${inout}"
      case other => "Unknown"
    }
  }

  def normalizeTableName(name: String): String = {
    var newName = name
    newName = newName.replaceAll("_[0-9a-fA-F]+$", "_X")
    newName = newName.replaceAll("[0-9]+", "N")
    newName
  }

  def printEdges(g: Graph): String = {
    val edge        = g.edges.toSeq.sorted(EdgeOrdering)
    val graphSymbol = edge.mkString(",")
    graphSymbol
  }
}
