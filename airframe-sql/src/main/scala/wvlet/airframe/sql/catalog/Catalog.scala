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
package wvlet.airframe.sql.catalog

import wvlet.airframe.sql.catalog.Catalog.CatalogDatabase
import wvlet.airframe.sql.catalog.DataType.NamedType
import wvlet.airframe.sql.model.Expression.QName



trait Catalog {
  import Catalog._

  def namespace: Option[String]

  def listDatabase: Seq[String]
  def getDatabase(database:String): CatalogDatabase
  def databaseExists(database: String): Boolean
  def createDatabase(catalogDatabase: CatalogDatabase): Unit

  def listTables(database:String): Seq[String]
  def getTable(database: String, table: String): CatalogTable
  def tableExists(database: String, table: String): Unit
  def createTable(database: String, table: CatalogTable): Unit

  def listFunctions: Seq[SQLFunction]
}






object Catalog {
  def schema: TableSchema                           = TableSchema(Seq.empty)
  def table(db: String, name: String): CatalogTable = CatalogTable(db, name, TableSchema(Seq.empty))

  def withTable(tbl: CatalogTable): Catalog = Catalog(Seq(tbl))

  case class TableSchema(columns: Seq[TableColumn]) {
    def addColumn(name: String, dataType: DataType, metadata: Map[String, Any] = Map.empty): TableSchema =
      this.copy(columns = columns :+ TableColumn(name, dataType, metadata))
  }

  case class TableColumn(name: String, dataType: DataType, metadata: Map[String, String] = Map.empty) {}

  /**
    * A database defined in the catalog
    * @param name
    * @param description
    * @param metadata
    */
  case class CatalogDatabase(name: String, description: String, properties: Map[String, Any] = Map.empty)

  case class CatalogTable(
    database: Option[String],
    name: String,
    schema: TableSchema,
    properties: Map[String, Any] = Map.empty
  ) {
    def fullName: String = s"${database.map(db => s"${db}.").getOrElse("")}.${name}"
  }

  case class Catalog(namespace: Option[String]=None, databaseTable: Map[CatalogTable, Seq[CatalogTable]]) {
    def addTable(tbl: CatalogTable): Catalog = this.copy((databaseTable +=  :+ tbl)

    def findTable(database: String, tableName: String): Option[CatalogTable] = {
      databases.find(x => x.db == database && x.name == tableName)
    }

    def findFromQName(contextDatabase: String, qname: QName): Option[CatalogTable] = {
      qname.parts match {
        case connector :: db :: tbl =>
          findTable(db, tbl.mkString("."))
        case db :: tbl =>
          findTable(db, tbl.mkString("."))
        case _ =>
          findTable(contextDatabase, qname.toString)
      }
    }
  }
}
