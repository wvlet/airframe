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
import wvlet.airframe.sql.catalog.Catalog
import wvlet.airframe.sql.catalog.DataType
import wvlet.airframe.sql.model.LogicalPlan.Relation

/**
  * The lowest level operator to access a table
  * @param table
  *   source table
  * @param columns
  *   projectec columns
  */
case class TableScan(table: Catalog.Table, columns: Seq[Catalog.TableColumn]) extends Relation with LeafPlan {
  override def inputAttributes: Seq[Attribute] = Seq.empty
  override def outputAttributes: Seq[Attribute] = {
    columns.map { col =>
      ResolvedAttribute(col.name, col.dataType, Some(table), Some(col))
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

case class ResolvedAttribute(
    name: String,
    dataType: DataType,
    sourceTable: Option[Catalog.Table],
    sourceColumn: Option[Catalog.TableColumn]
) extends Attribute {
  require(sourceTable.nonEmpty == sourceColumn.nonEmpty, "sourceTable and sourceColumn must be set together")

  def withAlias(newName: String): ResolvedAttribute = {
    this.copy(name = newName)
  }

  override def toString = {
    (sourceTable, sourceColumn) match {
      case (Some(t), Some(c)) if c.name == name =>
        s"${t.name}.${name}:${dataType}"
      case (Some(t), Some(c)) =>
        s"${name}:${dataType} <- ${t.name}.${c.name}"
      case _ =>
        s"${name}:${dataType}"
    }
    // s"${sourceTable.map(t => s"${t.name}.${name}").getOrElse(name)}:${dataType}"
  }
  override lazy val resolved = true
}

/**
  * For WITH cte as (...)
  * @param id
  * @param name
  * @param outputColumns
  */
case class CTERelationRef(name: String, outputColumns: Seq[Attribute]) extends Relation with LeafPlan {
  override def sig(config: QuerySignatureConfig): String = {
    if (config.embedTableNames)
      name
    else
      "T"
  }
  override def outputAttributes: Seq[Attribute] = outputColumns
}
