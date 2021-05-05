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
package wvlet.airframe.parquet

import wvlet.airframe.sql.model.LogicalPlan.{Relation, TableRef}
import wvlet.airframe.sql.model.{Expression, LogicalPlan}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.log.LogSupport

/**
  */
object ParquetQueryPlanner extends LogSupport {

  case class QueryPlan(
      sql: String,
      // projection target columns. If empty, select all columns (*)
      projectedColumns: Seq[String] = Seq.empty,
      condition: Map[String, Expression] = Map.empty
  ) {
    def selectAllColumns                         = this.copy(projectedColumns = Seq.empty)
    def selectColumns(columns: Seq[String])      = this.copy(projectedColumns = columns)
    def addCondition(cond: (String, Expression)) = this.copy(condition = condition + cond)
  }

  import LogicalPlan._
  import wvlet.airframe.sql.model.Expression._

  def parse(sql: String): QueryPlan = {
    val logicalPlan = SQLParser.parse(sql)

    val queryPlan = QueryPlan(sql)

    logicalPlan match {
      case Project(input, Seq(AllColumns(None))) =>
        parseRelation(queryPlan, input).selectAllColumns
      case Project(input, selectItems) =>
        val columns = selectItems.map {
          case SingleColumn(id: Identifier, _) =>
            id.value
          case other =>
            throw new IllegalArgumentException(s"Invalid select item: ${other}")
        }
        parseRelation(queryPlan, input).selectColumns(columns)
      case _ =>
        throw new IllegalArgumentException(s"Unsupported SQL expression: ${sql}")
    }
  }

  private def parseRelation(currentPlan: QueryPlan, relation: Relation): QueryPlan = {
    relation match {
      case TableRef(QName(Seq("_"))) =>
        currentPlan
      case Filter(input, expr) =>
        parseRelation(currentPlan, input)
    }
  }

}
