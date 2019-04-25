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
import wvlet.airframe.sql.catalog.Catalog.DbTable
import wvlet.airframe.sql.model.LogicalPlan.Relation

/**
  *
  */
case class TableScan(name: QName, table: DbTable) extends Relation with LeafPlan {

  /**
    * All child nodes of this plan node
    *
    * @return
    */
  override def inputAttributes: Seq[Attribute] = {
    table.table.schema.columns.map { c =>
      TypedAttribute(c.name, c.dataType)
    }
  }
  override def outputAttributes: Seq[Attribute] = inputAttributes
  override def sig(config: QuerySignatureConfig): String = {
    if (config.embedTableNames) {
      name.toString
    } else {
      "T"
    }
  }

  override lazy val resolved = true
}
