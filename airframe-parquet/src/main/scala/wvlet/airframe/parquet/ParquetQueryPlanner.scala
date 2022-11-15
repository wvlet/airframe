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

import org.apache.parquet.filter2.predicate.{FilterApi, FilterPredicate}
import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import wvlet.airframe.sql.model.{Expression, LogicalPlan}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.log.LogSupport

case class ParquetQueryPlan(
    sql: String,
    // projection target columns. If empty, select all columns (*)
    projectedColumns: Seq[String] = Seq.empty,
    predicate: Option[FilterPredicate] = None
) {
  def selectAllColumns                    = this.copy(projectedColumns = Seq.empty)
  def selectColumns(columns: Seq[String]) = this.copy(projectedColumns = columns)
  def predicate(pred: FilterPredicate)    = this.copy(predicate = Some(pred))
}

/**
  */
object ParquetQueryPlanner extends LogSupport {

  import LogicalPlan._
  import wvlet.airframe.sql.model.Expression._

  def parse(sql: String, schema: MessageType): ParquetQueryPlan = {
    new PlanBuilder(schema).parse(sql)
  }

  private class PlanBuilder(schema: MessageType) {
    def parse(sql: String): ParquetQueryPlan = {
      val logicalPlan = SQLParser.parse(sql)
      val queryPlan   = ParquetQueryPlan(sql)

      logicalPlan match {
        case Project(input, Seq(AllColumns(None, _)), _) =>
          parseRelation(input, queryPlan).selectAllColumns
        case Project(input, selectItems, _) =>
          val columns = selectItems.map {
            case SingleColumn(id: Identifier, _, _, _) =>
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
        case TableRef(QName(Seq("_"), _), _) =>
          currentPlan
        case Filter(input, expr, _) =>
          val pred = buildCondition(expr)
          parseRelation(input, currentPlan).predicate(pred)
      }
    }

    private def findParameterType(name: String): Option[PrimitiveTypeName] = {
      import scala.jdk.CollectionConverters._
      schema.getFields.asScala.find(f => f.getName == name && f.isPrimitive).map { f =>
        f.asPrimitiveType().getPrimitiveTypeName
      }
    }

    private def buildCondition(expr: Expression): FilterPredicate = {
      expr match {
        case ParenthesizedExpression(a, _) =>
          buildCondition(a)
        case Not(a, _) =>
          FilterApi.not(buildCondition(a))
        case And(a, b, _) =>
          FilterApi.and(buildCondition(a), buildCondition(b))
        case Or(a, b, _) =>
          FilterApi.or(buildCondition(a), buildCondition(b))
        // Parquet's FilterApi requires resolving value type when building operators, so we need to enumerate all possible patterns here
        // Eq
        case op @ Eq(a: Identifier, l: Literal, _) =>
          findParameterType(a.value) match {
            case Some(PrimitiveTypeName.INT32) =>
              FilterApi.eq(FilterApi.intColumn(a.value), java.lang.Integer.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.INT64) =>
              FilterApi.eq(FilterApi.longColumn(a.value), java.lang.Long.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.BOOLEAN) =>
              FilterApi.eq(FilterApi.booleanColumn(a.value), java.lang.Boolean.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.BINARY) =>
              FilterApi.eq(FilterApi.binaryColumn(a.value), Binary.fromString(l.stringValue))
            case Some(PrimitiveTypeName.FLOAT) =>
              FilterApi.eq(FilterApi.floatColumn(a.value), java.lang.Float.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.DOUBLE) =>
              FilterApi.eq(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(l.stringValue))
            case Some(other) =>
              throw new IllegalArgumentException(s"Unsupported type ${other}: ${op}")
            case _ =>
              throw new IllegalArgumentException(s"Unknown column ${a.value}: ${op}")
          }
        case op @ NotEq(a: Identifier, l: Literal, _) =>
          findParameterType(a.value) match {
            case Some(PrimitiveTypeName.INT32) =>
              FilterApi.notEq(FilterApi.intColumn(a.value), java.lang.Integer.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.INT64) =>
              FilterApi.notEq(FilterApi.longColumn(a.value), java.lang.Long.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.BOOLEAN) =>
              FilterApi.notEq(FilterApi.booleanColumn(a.value), java.lang.Boolean.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.BINARY) =>
              FilterApi.notEq(FilterApi.binaryColumn(a.value), Binary.fromString(l.stringValue))
            case Some(PrimitiveTypeName.FLOAT) =>
              FilterApi.notEq(FilterApi.floatColumn(a.value), java.lang.Float.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.DOUBLE) =>
              FilterApi.notEq(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(l.stringValue))
            case Some(other) =>
              throw new IllegalArgumentException(s"Unsupported type ${other}: ${op}")
            case _ =>
              throw new IllegalArgumentException(s"Unknown column ${a.value}: ${op}")
          }
        case op @ LessThan(a: Identifier, l: Literal, _) =>
          findParameterType(a.value) match {
            case Some(PrimitiveTypeName.INT32) =>
              FilterApi.lt(FilterApi.intColumn(a.value), java.lang.Integer.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.INT64) =>
              FilterApi.lt(FilterApi.longColumn(a.value), java.lang.Long.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.BINARY) =>
              FilterApi.lt(FilterApi.binaryColumn(a.value), Binary.fromString(l.stringValue))
            case Some(PrimitiveTypeName.FLOAT) =>
              FilterApi.lt(FilterApi.floatColumn(a.value), java.lang.Float.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.DOUBLE) =>
              FilterApi.lt(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(l.stringValue))
            case Some(other) =>
              throw new IllegalArgumentException(s"Unsupported type ${other}: ${op}")
            case _ =>
              throw new IllegalArgumentException(s"Unknown column ${a.value}: ${op}")
          }
        case op @ LessThanOrEq(a: Identifier, l: Literal, _) =>
          findParameterType(a.value) match {
            case Some(PrimitiveTypeName.INT32) =>
              FilterApi.ltEq(FilterApi.intColumn(a.value), java.lang.Integer.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.INT64) =>
              FilterApi.ltEq(FilterApi.longColumn(a.value), java.lang.Long.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.BINARY) =>
              FilterApi.ltEq(FilterApi.binaryColumn(a.value), Binary.fromString(l.stringValue))
            case Some(PrimitiveTypeName.FLOAT) =>
              FilterApi.ltEq(FilterApi.floatColumn(a.value), java.lang.Float.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.DOUBLE) =>
              FilterApi.ltEq(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(l.stringValue))
            case Some(other) =>
              throw new IllegalArgumentException(s"Unsupported type ${other}: ${op}")
            case _ =>
              throw new IllegalArgumentException(s"Unknown column ${a.value}: ${op}")
          }
        case op @ GreaterThan(a: Identifier, l: Literal, _) =>
          findParameterType(a.value) match {
            case Some(PrimitiveTypeName.INT32) =>
              FilterApi.gt(FilterApi.intColumn(a.value), java.lang.Integer.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.INT64) =>
              FilterApi.gt(FilterApi.longColumn(a.value), java.lang.Long.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.BINARY) =>
              FilterApi.gt(FilterApi.binaryColumn(a.value), Binary.fromString(l.stringValue))
            case Some(PrimitiveTypeName.FLOAT) =>
              FilterApi.gt(FilterApi.floatColumn(a.value), java.lang.Float.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.DOUBLE) =>
              FilterApi.gt(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(l.stringValue))
            case Some(other) =>
              throw new IllegalArgumentException(s"Unsupported type ${other}: ${op}")
            case _ =>
              throw new IllegalArgumentException(s"Unknown column ${a.value}: ${op}")
          }
        case op @ GreaterThanOrEq(a: Identifier, l: Literal, _) =>
          findParameterType(a.value) match {
            case Some(PrimitiveTypeName.INT32) =>
              FilterApi.gtEq(FilterApi.intColumn(a.value), java.lang.Integer.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.INT64) =>
              FilterApi.gtEq(FilterApi.longColumn(a.value), java.lang.Long.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.BINARY) =>
              FilterApi.gtEq(FilterApi.binaryColumn(a.value), Binary.fromString(l.stringValue))
            case Some(PrimitiveTypeName.FLOAT) =>
              FilterApi.gtEq(FilterApi.floatColumn(a.value), java.lang.Float.valueOf(l.stringValue))
            case Some(PrimitiveTypeName.DOUBLE) =>
              FilterApi.gtEq(FilterApi.doubleColumn(a.value), java.lang.Double.valueOf(l.stringValue))
            case Some(other) =>
              throw new IllegalArgumentException(s"Unsupported type ${other}: ${op}")
            case _ =>
              throw new IllegalArgumentException(s"Unknown column ${a.value}: ${op}")
          }
        case op @ Between(a: Identifier, b, c, _) =>
          buildCondition(
            And(GreaterThanOrEq(a, b, op.nodeLocation), LessThanOrEq(a, c, op.nodeLocation), op.nodeLocation)
          )
        case op @ IsNull(a: Identifier, _) =>
          findParameterType(a.value) match {
            case Some(PrimitiveTypeName.INT32) =>
              FilterApi.eq(FilterApi.intColumn(a.value), null.asInstanceOf[java.lang.Integer])
            case Some(PrimitiveTypeName.INT64) =>
              FilterApi.eq(FilterApi.longColumn(a.value), null.asInstanceOf[java.lang.Long])
            case Some(PrimitiveTypeName.BOOLEAN) =>
              FilterApi.eq(FilterApi.booleanColumn(a.value), null.asInstanceOf[java.lang.Boolean])
            case Some(PrimitiveTypeName.BINARY) =>
              FilterApi.eq(FilterApi.binaryColumn(a.value), null.asInstanceOf[Binary])
            case Some(PrimitiveTypeName.FLOAT) =>
              FilterApi.eq(FilterApi.floatColumn(a.value), null.asInstanceOf[java.lang.Float])
            case Some(PrimitiveTypeName.DOUBLE) =>
              FilterApi.eq(FilterApi.doubleColumn(a.value), null.asInstanceOf[java.lang.Double])
            case Some(other) =>
              throw new IllegalArgumentException(s"Unsupported type ${other}: ${op}")
            case _ =>
              throw new IllegalArgumentException(s"Unknown column ${a.value}: ${op}")
          }
        case op @ IsNotNull(a: Identifier, _) =>
          findParameterType(a.value) match {
            case Some(PrimitiveTypeName.INT32) =>
              FilterApi.notEq(FilterApi.intColumn(a.value), null.asInstanceOf[java.lang.Integer])
            case Some(PrimitiveTypeName.INT64) =>
              FilterApi.notEq(FilterApi.longColumn(a.value), null.asInstanceOf[java.lang.Long])
            case Some(PrimitiveTypeName.BOOLEAN) =>
              FilterApi.notEq(FilterApi.booleanColumn(a.value), null.asInstanceOf[java.lang.Boolean])
            case Some(PrimitiveTypeName.BINARY) =>
              FilterApi.notEq(FilterApi.binaryColumn(a.value), null.asInstanceOf[Binary])
            case Some(PrimitiveTypeName.FLOAT) =>
              FilterApi.notEq(FilterApi.floatColumn(a.value), null.asInstanceOf[java.lang.Float])
            case Some(PrimitiveTypeName.DOUBLE) =>
              FilterApi.notEq(FilterApi.doubleColumn(a.value), null.asInstanceOf[java.lang.Double])
            case Some(other) =>
              throw new IllegalArgumentException(s"Unsupported type ${other}: ${op}")
            case _ =>
              throw new IllegalArgumentException(s"Unknown column ${a.value}: ${op}")
          }
        case other =>
          throw new IllegalArgumentException(s"Unsupported operator: ${other}")
      }
    }
  }
}
