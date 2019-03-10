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
import wvlet.airframe.sql.model.LogicalPlan
import wvlet.airframe.sql.parser.SQLParser

/**
  * Graph for representing table input and output dependencies
  */
object TableGraph {
  val empty = Graph(Set.empty, Set.empty)

  def of(sql: String): Graph = {
    val p = SQLParser.parse(sql)
    of(p)
  }

  def of(p: LogicalPlan): Graph = {
    val finder = new InOutTableFinder
    finder.process(p, TableScanContext(Some(Terminal)))
    finder.graph
  }

  case object Terminal extends Node {
    override def toString = name
    def name              = "#"
  }
  case class Alias(name: String) extends Node {
    override def toString = s"&${name}"
  }
  case class SourceTable(name: String) extends Node {
    override def toString = name
  }
  case class TargetTable(name: String) extends Node {
    override def toString = s"!${name}"
  }

}
