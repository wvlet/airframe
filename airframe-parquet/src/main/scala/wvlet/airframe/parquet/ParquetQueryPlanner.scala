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

import org.apache.parquet.filter2.predicate.FilterApi
import wvlet.airframe.sql.model.LogicalPlan.{Relation, TableRef}
import wvlet.airframe.sql.model.{Expression, LogicalPlan}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.log.LogSupport

case class ParquetQueryPlan(
    sql: String,
    // projection target columns. If empty, select all columns (*)
    projectedColumns: Seq[String] = Seq.empty,
    condition: Option[Expression] = None
) {
  def selectAllColumns                    = this.copy(projectedColumns = Seq.empty)
  def selectColumns(columns: Seq[String]) = this.copy(projectedColumns = columns)
  def addCondition(cond: Expression)      = this.copy(condition = Some(cond))
}

/**
  */
object ParquetQueryPlanner extends LogSupport {

  import LogicalPlan._
  import wvlet.airframe.sql.model.Expression._

  def parse(sql: String): ParquetQueryPlan = {
    val logicalPlan = SQLParser.parse(sql)

    val queryPlan = ParquetQueryPlan(sql)

    logicalPlan match {
      case Project(input, Seq(AllColumns(None))) =>
        parseRelation(input, queryPlan).selectAllColumns
      case Project(input, selectItems) =>
        val columns = selectItems.map {
          case SingleColumn(id: Identifier, _) =>
            id.value
          case other =>
            throw new IllegalArgumentException(s"Invalid select item: ${other}")
        }
        parseRelation(input, queryPlan).selectColumns(columns)
      case _ =>
        throw new IllegalArgumentException(s"Unsupported SQL expression: ${sql}")
    }
  }

  private def parseRelation(relation: Relation, currentPlan: ParquetQueryPlan): ParquetQueryPlan = {
    relation match {
      case TableRef(QName(Seq("_"))) =>
        currentPlan
      case Filter(input, expr) =>
        verifyCondition(expr)
        parseRelation(input, currentPlan).addCondition(expr)
    }
  }

  private def verifyCondition(expr: Expression): Unit = {
    expr match {
      case UnquotedIdentifier(_) =>
      // ok
      case BackQuotedIdentifier(_) =>
      // ok
      case QuotedIdentifier(_) =>
      // ok
      case NullLiteral | TrueLiteral | FalseLiteral =>
      // ok
      case StringLiteral(_) =>
      // ok
      case LongLiteral(_) =>
      // ok
      case DoubleLiteral(_) =>
      // ok
      case Not(a) =>
        verifyCondition(a)
      case Eq(a, b) =>
        verifyCondition(a)
        verifyCondition(b)
      case NotEq(a, b) =>
        verifyCondition(a)
        verifyCondition(b)
      case And(a, b) =>
        verifyCondition(a)
        verifyCondition(b)
      case Or(a, b) =>
        verifyCondition(a)
        verifyCondition(b)
      case LessThan(a, b) =>
        verifyCondition(a)
        verifyCondition(b)
      case LessThanOrEq(a, b) =>
        verifyCondition(a)
        verifyCondition(b)
      case GreaterThan(a, b) =>
        verifyCondition(a)
        verifyCondition(b)
      case GreaterThanOrEq(a, b) =>
        verifyCondition(a)
        verifyCondition(b)
      case Between(a, b, c) =>
        verifyCondition(a)
        verifyCondition(b)
        verifyCondition(c)
      case IsNull(a) =>
        verifyCondition(a)
      case IsNotNull(a) =>
        verifyCondition(a)
//      case In(a, lst) =>
//        verifyCondition(a)
//        lst.foreach(verifyCondition(_))
//      case NotIn(a, lst) =>
//        verifyCondition(a)
//        lst.foreach(verifyCondition(_))
      case other =>
        throw new IllegalArgumentException(s"Unsupported operator: ${other}")
    }
  }

}
