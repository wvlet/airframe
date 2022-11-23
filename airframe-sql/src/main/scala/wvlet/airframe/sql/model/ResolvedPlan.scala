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
import wvlet.airframe.sql.model.LogicalPlan.Relation

/**
  * The lowest level operator to access a table
  * @param table
  *   source table
  * @param columns
  *   projectec columns
  */
case class TableScan(table: Catalog.Table, columns: Seq[Catalog.TableColumn], nodeLocation: Option[NodeLocation])
    extends Relation
    with LeafPlan {
  override def inputAttributes: Seq[Attribute] = Seq.empty
  override def outputAttributes: Seq[Attribute] = {
    columns.map { col =>
      ResolvedAttribute(
        col.name,
        col.dataType,
        None,
        Seq(SourceColumn(table, col)),
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

case class Alias(name: String, resolvedAttribute: ResolvedAttribute)

case class SourceColumn(table: Catalog.Table, column: Catalog.TableColumn) {
  def fullName: String = s"${table.name}.${column.name}"
}

case class ResolvedAttribute(
    name: String,
    dataType: DataType,
    qualifier: Option[String],
    sourceColumns: Seq[SourceColumn],
    nodeLocation: Option[NodeLocation]
) extends Attribute {

  def withAlias(newName: String): ResolvedAttribute = {
    this.copy(name = newName)
  }

  def relationNames: Seq[String] = qualifier match {
    case Some(q) => Seq(q)
    case _       => sourceColumns.map(_.table.name)
  }

  /**
    * Returns true if this resolved attribute matches with a given table name and colum name
    */
  def matchesWith(tableName: String, columnName: String): Boolean = {
    relationNames match {
      case Nil => columnName == name
      case tableNames =>
        tableNames.exists { tbl =>
          tbl == tableName && columnName == name
        }
    }
  }

  override def toString = {
    (qualifier, sourceColumns) match {
      case (Some(q), columns) if columns.nonEmpty =>
        columns
          .map(_.fullName)
          .mkString(s"${q},${name}:${dataType} <- ", ", ", "")
      case (None, columns) if columns.nonEmpty =>
        columns
          .map(_.fullName)
          .mkString(s"${name}:${dataType} <- ", ", ", "")
      case _ =>
        s"${name}:${dataType}"
    }
  }
  override lazy val resolved = true

  override def withQualifier(newQualifier: String): Attribute = {
    this.copy(qualifier = Some(newQualifier))
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
