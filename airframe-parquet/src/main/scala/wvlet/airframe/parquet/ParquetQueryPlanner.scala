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

import org.apache.parquet.filter2.predicate.Operators.Column
import org.apache.parquet.filter2.predicate.{FilterApi, FilterPredicate}
import org.apache.parquet.io.api.Binary
import wvlet.airframe.sql.model.LogicalPlan.{Relation, TableRef}
import wvlet.airframe.sql.model.{Expression, LogicalPlan}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.airframe.surface.Primitive
import wvlet.airframe.surface.Primitive.PrimitiveSurface
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
        buildCondition(expr)
        parseRelation(input, currentPlan).addCondition(expr)
    }
  }

//  case UnquotedIdentifier(_) =>
//  // ok
//  case BackQuotedIdentifier(_) =>
//  // ok
//  case QuotedIdentifier(_) =>
//  // ok

  private def inferType(e: Expression): PrimitiveSurface = {
    e match {
      case TrueLiteral => Primitive.Boolean
      case FalseLiteral =>
        Primitive.Boolean
      case StringLiteral(s) =>
        Primitive.String
      case LongLiteral(l) =>
        Primitive.Long
      case DoubleLiteral(d) =>
        Primitive.Double
      case other =>
        throw new IllegalArgumentException(s"Unsupported literal: ${other}")
    }
  }

  implicit class RichSurface(val p: PrimitiveSurface) extends AnyVal {
    def toParquetColumn(columnName: String): Column[_] = p match {
      case Primitive.String =>
        FilterApi.binaryColumn(columnName)
      case Primitive.Double =>
        FilterApi.doubleColumn(columnName)
      case Primitive.Long =>
        FilterApi.longColumn(columnName)
      case Primitive.Boolean =>
        FilterApi.booleanColumn(columnName)
      case _ =>
        throw new IllegalArgumentException(s"Unsupported column type: ${p}")
    }
  }

  private def buildCondition(expr: Expression): FilterPredicate = {
    expr match {
      case Not(a) =>
        FilterApi.not(buildCondition(a))
      case And(a, b) =>
        FilterApi.and(buildCondition(a), buildCondition(b))
      case Or(a, b) =>
        FilterApi.or(buildCondition(a), buildCondition(b))
      // Parquet's FilterApi requires resolving value type when building operators, so we need to enumerate all possible patterns here
      // Eq
      case Eq(a: Identifier, b: BooleanLiteral) =>
        FilterApi.eq(FilterApi.booleanColumn(a.value), java.lang.Boolean.valueOf(b.booleanValue))
      case Eq(a: Identifier, StringLiteral(s)) =>
        FilterApi.eq(FilterApi.binaryColumn(a.value), Binary.fromString(s))
      case Eq(a: Identifier, LongLiteral(s)) =>
        FilterApi.eq(FilterApi.longColumn(a.value), java.lang.Long.valueOf(s))
      case Eq(a: Identifier, DoubleLiteral(s)) =>
        FilterApi.eq(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(s))
      // NotEq
      case NotEq(a: Identifier, b: BooleanLiteral) =>
        FilterApi.notEq(FilterApi.booleanColumn(a.value), java.lang.Boolean.valueOf(b.booleanValue))
      case NotEq(a: Identifier, StringLiteral(s)) =>
        FilterApi.notEq(FilterApi.binaryColumn(a.value), Binary.fromString(s))
      case NotEq(a: Identifier, LongLiteral(s)) =>
        FilterApi.notEq(FilterApi.longColumn(a.value), java.lang.Long.valueOf(s))
      case NotEq(a: Identifier, DoubleLiteral(s)) =>
        FilterApi.notEq(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(s))
      // LessThan
      case LessThan(a: Identifier, StringLiteral(s)) =>
        FilterApi.lt(FilterApi.binaryColumn(a.value), Binary.fromString(s))
      case LessThan(a: Identifier, LongLiteral(s)) =>
        FilterApi.lt(FilterApi.longColumn(a.value), java.lang.Long.valueOf(s))
      case LessThan(a: Identifier, DoubleLiteral(s)) =>
        FilterApi.lt(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(s))
      // LessThanOrEq
      case LessThanOrEq(a: Identifier, StringLiteral(s)) =>
        FilterApi.ltEq(FilterApi.binaryColumn(a.value), Binary.fromString(s))
      case LessThanOrEq(a: Identifier, LongLiteral(s)) =>
        FilterApi.ltEq(FilterApi.longColumn(a.value), java.lang.Long.valueOf(s))
      case LessThanOrEq(a: Identifier, DoubleLiteral(s)) =>
        FilterApi.ltEq(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(s))
      // GreaterThan
      case GreaterThan(a: Identifier, StringLiteral(s)) =>
        FilterApi.gt(FilterApi.binaryColumn(a.value), Binary.fromString(s))
      case GreaterThan(a: Identifier, LongLiteral(s)) =>
        FilterApi.gt(FilterApi.longColumn(a.value), java.lang.Long.valueOf(s))
      case GreaterThan(a: Identifier, DoubleLiteral(s)) =>
        FilterApi.gt(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(s))
      // GreaterThanOrEq
      case GreaterThanOrEq(a: Identifier, StringLiteral(s)) =>
        FilterApi.gtEq(FilterApi.binaryColumn(a.value), Binary.fromString(s))
      case GreaterThanOrEq(a: Identifier, LongLiteral(s)) =>
        FilterApi.gtEq(FilterApi.longColumn(a.value), java.lang.Long.valueOf(s))
      case GreaterThanOrEq(a: Identifier, DoubleLiteral(s)) =>
        FilterApi.gtEq(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(s))
      case Between(a: Identifier, b, c) =>
        buildCondition(And(GreaterThanOrEq(a, b), LessThanOrEq(a, c)))
      //case IsNull(a) =>
      //case IsNotNull(a) =>
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
