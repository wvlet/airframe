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
package wvlet.airframe.sql.model

import wvlet.airframe.sql.analyzer.QuerySignatureConfig
import wvlet.airframe.sql.catalog.{Catalog, DataType}
import wvlet.airframe.sql.model.Expression.Identifier
import wvlet.airframe.sql.model.LogicalPlan.Relation
import wvlet.log.LogSupport

/**
  * The lowest level operator to access a table
  * @param fullName
  *   original table reference name in SQL. Used for generating SQL text
  *
  * @param table
  *   source table
  * @param columns
  *   projectec columns
  */
case class TableScan(
    fullName: String,
    table: Catalog.Table,
    columns: Seq[Catalog.TableColumn],
    nodeLocation: Option[NodeLocation]
) extends Relation
    with LeafPlan {
  override def inputAttributes: Seq[Attribute] = Seq.empty
  override def outputAttributes: Seq[Attribute] = {
    columns.map { col =>
      ResolvedAttribute(
        col.name,
        col.dataType,
        None, // This must be None first
        Some(SourceColumn(table, col)),
        None // ResolvedAttribute always has no NodeLocation
      )
    }
  }
  override def sig(config: QuerySignatureConfig): String = {
    if (config.embedTableNames) {
      table.fullName
    } else {
      "T"
    }
  }

  override lazy val resolved = true
}

case class SourceColumn(table: Catalog.Table, column: Catalog.TableColumn) {
  def fullName: String = s"${table.name}.${column.name}"
}

case class ResolvedAttribute(
    name: String,
    override val dataType: DataType,
    // user-given qualifier
    qualifier: Option[String],
    // If this attribute directly refers to a table column, its source column will be set.
    sourceColumn: Option[SourceColumn],
    nodeLocation: Option[NodeLocation]
) extends Attribute
    with LogSupport {

  override lazy val resolved   = true
  override def sqlExpr: String = s"${prefix}${name}"

  override def withQualifier(newQualifier: Option[String]): Attribute = {
    this.copy(qualifier = newQualifier)
  }
  override def inputColumns: Seq[Attribute] = Seq(this)

  override def toString = {
    sourceColumn match {
      case Some(c) =>
        s"*${prefix}${typeDescription} <- ${c.fullName}"
      case None =>
        s"*${prefix}${typeDescription}"
    }
  }

  override def sourceColumns: Seq[SourceColumn] = {
    sourceColumn.toSeq
  }
}

/**
  * For WITH cte as (...)
  * @param id
  * @param name
  * @param outputColumns
  */
case class CTERelationRef(name: String, outputColumns: Seq[Attribute], nodeLocation: Option[NodeLocation])
    extends Relation
    with LeafPlan {
  override def sig(config: QuerySignatureConfig): String = {
    if (config.embedTableNames)
      name
    else
      "T"
  }
  override def outputAttributes: Seq[Attribute] = outputColumns
}

