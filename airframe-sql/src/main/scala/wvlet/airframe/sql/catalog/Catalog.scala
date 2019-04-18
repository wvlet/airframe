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

import wvlet.airframe.sql.model.QName

object Catalog {

  case class TableSchema(columns: Seq[NamedType])
  case class Table(name: String, schema: TableSchema)
  case class DbTable(db: String, table: Table)

  case class Catalog(databases: Seq[DbTable]) {

    def findTable(database: String, tableName: String): Option[DbTable] = {
      databases.find(x => x.db == database && x.table.name == tableName)
    }

    def findFromQName(contextDatabase: String, qname: QName): Option[DbTable] = {
      qname.parts match {
        case connetor :: db :: tbl =>
          findTable(db, tbl.mkString("."))
        case db :: tbl =>
          findTable(db, tbl.mkString("."))
        case _ =>
          findTable(contextDatabase, qname.toString)
      }
    }
  }

}
