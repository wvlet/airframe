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

class InMemoryCatalog(namespace: String) extends Catalog {

  override def namespace: Option[String] = ???

  override def listDatabase: Seq[String] = ???

  override def getDatabase(database: String): Catalog.CatalogDatabase = ???

  override def databaseExists(database: String): Boolean = ???

  override def createDatabase(catalogDatabase: Catalog.CatalogDatabase): Unit = ???

  override def listTables(database: String): Seq[String] = ???

  override def getTable(database: String, table: String): Catalog.CatalogTable = ???

  override def tableExists(database: String, table: String): Unit = ???

  override def createTable(database: String, table: Catalog.CatalogTable): Unit = ???

  override def listFunctions: Seq[SQLFunction] = ???
}
