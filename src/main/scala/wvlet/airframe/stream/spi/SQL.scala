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
package wvlet.airframe.stream.spi

import wvlet.surface.Surface

/**
  *
  */
object SQL {

  trait Schema
  case class AnonSchema(columns: Seq[Column])                extends Schema
  case class TableSchema(name: String, columns: Seq[Column]) extends Schema

  case class Column(name: String, columnType: DataType)

  sealed trait DataType
  case object IntegerType                                    extends DataType
  case object FloatType                                      extends DataType
  case object BooleanType                                    extends DataType
  case object StringType                                     extends DataType
  case object JSONType                                       extends DataType
  case object BinaryType                                     extends DataType
  case object NullType                                       extends DataType
  case object TimestampType                                  extends DataType
  case class ObjectType(surface: Surface)                    extends DataType
  case class AraryType(elemType: DataType)                   extends DataType
  case class MapType(keyType: DataType, valueType: DataType) extends DataType

  trait Relation
  case class TableScan(schema: TableSchema)                     extends Relation
  case class TableScanFilter(schema: TableSchema, filter: Cond) extends Relation
  case class RawSQL(sql: String, schema: Schema)                extends Relation

  case class Project(in: Relation, schema: Seq[Expression])                                      extends Relation
  case class Join(left: Relation, right: Relation, joinType: JoinType, cond: Cond)               extends Relation
  case class Aggregate(in: Relation, keys: Seq[Expression], aggregate: Seq[AggregateExpression]) extends Relation
  case class Sort(in: Relation, sortKeys: Seq[Expression])                                       extends Relation

  sealed trait JoinType
  case object InnerJoin
  case object LeftOuterJoin
  case object RightOuterJoin
  case object FullOuterJoin
  case object CrossJoin

  trait Expression
  sealed trait Ref                   extends Expression
  case class ColumnRef(name: String) extends Ref

  sealed trait Cond                                        extends Expression
  case object NoOp                                         extends Cond
  case class Eq(a: Expression, b: Expression)              extends Cond
  case class And(a: Expression, b: Expression)             extends Cond
  case class Or(a: Expression, b: Expression)              extends Cond
  case class Not(expr: Expression)                         extends Cond
  case class LessThan(a: Expression, b: Expression)        extends Cond
  case class LessThanOrEq(a: Expression, b: Expression)    extends Cond
  case class GreaterThan(a: Expression, b: Expression)     extends Cond
  case class GreaterThanOrEq(a: Expression, b: Expression) extends Cond

  case class AggregateExpression(func: AggregateFunction, mode: AggregateMode, isDistinct: Boolean) extends Expression

  sealed trait AggregateMode
  case object PartialAggregate extends AggregateMode
  case object FinalAggregate   extends AggregateMode
  case object FullAggregate    extends AggregateMode

  sealed trait AggregateFunction
  case class Sum(e: Expression)                      extends AggregateFunction
  case class Avg(e: Expression)                      extends AggregateFunction
  case class Count(e: Expression)                    extends AggregateFunction
  case class CountDistinct(e: Expression)            extends AggregateFunction
  case class CountMinSketch(e: Expression)           extends AggregateFunction
  case class CountApproximateDistinct(e: Expression) extends AggregateFunction
  case class ApproximatePercentile(e: Expression)    extends AggregateFunction
  case class Min(e: Expression)                      extends AggregateFunction
  case class Max(e: Expression)                      extends AggregateFunction
  case class First(e: Expression)                    extends AggregateFunction
  case class Last(e: Expression)                     extends AggregateFunction

}
