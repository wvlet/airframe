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

import wvlet.airframe.sql.model.Expression.QName

object Catalog {

  def schema: TableSchema                      = TableSchema(Seq.empty)
  def table(db: String, name: String): DbTable = DbTable(db, name, TableSchema(Seq.empty))

  def withTable(tbl: DbTable): Catalog = Catalog(Seq(tbl))

  case class TableSchema(columns: Seq[NamedType]) {
    def addColumn(name: String, dataType: DataType): TableSchema =
      this.copy(columns = columns :+ NamedType(name, dataType))
  }

  case class DbTable(db: String = "default", name: String, schema: TableSchema) {
    def fullName: String = s"${db}.${name}"

    def addColumn(name: String, dataType: DataType) = this.copy(schema = schema.addColumn(name, dataType))

    def withDatabase(db: String)        = this.copy(db = db)
    def withName(name: String)          = this.copy(name = name)
    def withSchema(schema: TableSchema) = this.copy(schema = schema)
  }

  case class Catalog(databases: Seq[DbTable]) {
    def addTable(tbl: DbTable): Catalog = Catalog(databases :+ tbl)

    def findTable(database: String, tableName: String): Option[DbTable] = {
      databases.find(x => x.db == database && x.name == tableName)
    }

    def findFromQName(contextDatabase: String, qname: QName): Option[DbTable] = {
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
