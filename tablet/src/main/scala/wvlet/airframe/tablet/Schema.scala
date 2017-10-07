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
package wvlet.airframe.tablet

object Schema {
  sealed trait ColumnType {
    //def isPrimitive : Boolean
    //def javaType: Class[_]
  }

  sealed trait PrimitiveType extends ColumnType
  sealed trait StructureType extends ColumnType

  case object NIL                                            extends PrimitiveType
  case object ANY                                            extends PrimitiveType
  case object INTEGER                                        extends PrimitiveType
  case object FLOAT                                          extends PrimitiveType
  case object BOOLEAN                                        extends PrimitiveType
  case object STRING                                         extends PrimitiveType
  case object TIMESTAMP                                      extends PrimitiveType
  case object BINARY                                         extends PrimitiveType
  case object JSON                                           extends PrimitiveType
  case class ARRAY(elemType: ColumnType)                     extends StructureType
  case class MAP(keyType: ColumnType, valueType: ColumnType) extends StructureType
  case class RECORD(column: Seq[Column])                     extends StructureType

  object ColumnType {
    def unapply(s: String): Option[ColumnType] = {
      val tpe = s match {
        case "varchar" => STRING
        case "string"  => STRING
        case "bigint"  => INTEGER
        case "double"  => FLOAT
        case "float"   => FLOAT
        case "json"    => STRING // TODO jse JSON type
        case _         => STRING // TODO support more type
      }
      Some(tpe)
    }
  }
}

case class Column(
    // 0-origin index
    index: Int,
    name: String,
    dataType: Schema.ColumnType
) {
  override def toString = s"${index}:${name} ${dataType}"
}

case class Schema(name: String, column: Seq[Column]) {
  override def toString: String = s"$name(${column.mkString(", ")})"

  private lazy val columnIdx: Map[Column, Int] = column.zipWithIndex.toMap[Column, Int]

  def size: Int = column.size

  /**
    * @param index 0-origin index
    * @return
    */
  def columnType(index: Int) = column(index)

  /**
    * Return the column index
    *
    * @param column
    * @return
    */
  def columnIndex(column: Column) = columnIdx(column)
}
