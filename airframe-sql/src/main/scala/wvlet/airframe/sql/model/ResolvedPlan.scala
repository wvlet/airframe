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
  */
case class TableScan(table: Catalog.Table, columns: Seq[String]) extends Relation with LeafPlan {
  override def inputAttributes: Seq[Attribute] = Seq.empty
  override def outputAttributes: Seq[Attribute] = {
    columns.flatMap { col =>
      table.schema.columns.find(_.name == col).map { c => ResolvedAttribute(c.name, c.dataType, Some(table), Some(c)) }
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
  override def toString      = s"${sourceTable.map(t => s"${t.name}.${name}").getOrElse(name)}:${dataType}"
  override lazy val resolved = true
}
