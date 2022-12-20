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
        Some(table.name),
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

case class Alias(name: String, resolvedAttribute: ResolvedAttribute)

case class SourceColumn(table: Catalog.Table, column: Catalog.TableColumn) {
  def fullName: String = s"${table.name}.${column.name}"
}

case class ResolvedAttribute(
    name: String,
    override val dataType: DataType,
    // table name
    qualifier: Option[String],
    // If this attribute directly refers to a table column, its source column will be set.
    sourceColumn: Option[SourceColumn],
    nodeLocation: Option[NodeLocation]
) extends Attribute {

  override def sqlExpr: String = {
    s"${qualifier.map(q => s"${q}.").getOrElse("")}${name}"
  }

  override def alias: Option[String] = Some(name)
  def withAlias(newName: String): ResolvedAttribute = {
    this.copy(name = newName)
  }
  override def withQualifier(newQualifier: String): Attribute = {
    this.copy(qualifier = Some(newQualifier))
  }

  override def inputColumns: Seq[Attribute] = Seq(this)

  def relationNames: Seq[String] = qualifier match {
    case Some(q) => Seq(q)
    case _       => sourceColumn.map(_.table.name).toSeq
  }

//  /**
//    * Returns true if this resolved attribute matches with a given table name and colum name
//    */
//  def matchesWith(tableName: String, columnName: String): Boolean = {
//    relationNames match {
//      case Nil => columnName == name
//      case tableNames =>
//        tableNames.exists { tbl =>
//          tbl == tableName && matchesWith(columnName)
//        }
//    }
//  }
//
//  def matchesWith(columnName: String): Boolean = {
//    name == columnName
//  }

  override def toString = {
    (qualifier, sourceColumn) match {
      case (Some(q), columns) if columns.nonEmpty =>
        columns
          .map(_.fullName)
          .mkString(s"${q}.${typeDescription} <- [", ", ", "]")
      case (None, columns) if columns.nonEmpty =>
        columns
          .map(_.fullName)
          .mkString(s"${typeDescription} <- [", ", ", "]")
      case _ =>
        s"${typeDescription}"
    }
  }
  override lazy val resolved = true
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
