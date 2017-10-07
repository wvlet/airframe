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
package wvlet.airframe.tablet.msgpack

import wvlet.airframe.tablet.Schema
import wvlet.airframe.tablet.Schema.ColumnType

/**
  *
  */
class MessageHolder {
  private var b: Boolean = false
  private var l: Long    = 0L
  private var d: Double  = 0d
  private var s: String  = ""
  private var o: AnyRef  = null

  private var valueType: ColumnType = Schema.NIL

  def isNull: Boolean = valueType == Schema.NIL

  def getLong: Long       = l
  def getBoolean: Boolean = b
  def getDouble: Double   = d
  def getString: String   = s
  def getObject: AnyRef   = o

  def getLastValue: Any = {
    if (isNull) {
      null
    } else {
      valueType match {
        case Schema.NIL =>
          null
        case Schema.INTEGER =>
          this.getLong
        case Schema.FLOAT =>
          this.getDouble
        case Schema.STRING =>
          this.getString
        case Schema.BOOLEAN =>
          this.getBoolean
        case _ =>
          getObject
      }
    }
  }

  def setLong(v: Long): Long = {
    l = v
    valueType = Schema.INTEGER
    v
  }

  def setBoolean(v: Boolean): Boolean = {
    b = v
    valueType = Schema.BOOLEAN
    v
  }

  def setDouble(v: Double): Double = {
    d = v
    valueType = Schema.FLOAT
    v
  }

  def setString(v: String): String = {
    s = v
    valueType = Schema.STRING
    v
  }

  def setObject[A](v: A): A = {
    o = v.asInstanceOf[AnyRef]
    valueType = Schema.ANY
    v
  }

  def setNull {
    valueType = Schema.NIL
  }
}
