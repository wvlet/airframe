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

import org.msgpack.value.ValueType
import wvlet.airframe.tablet.Schema
import wvlet.airframe.tablet.Schema.ColumnType
import wvlet.airframe.tablet.msgpack.MessageCodec.INVALID_DATA

/**
  *
  */
class MessageHolder {
  private var valueType: ColumnType = Schema.NIL

  private var b: Boolean             = false
  private var l: Long                = 0L
  private var d: Double              = 0d
  private var s: String              = ""
  private var o: AnyRef              = null
  private var err: Option[Throwable] = None

  def isNull: Boolean   = valueType == Schema.NIL
  def hasError: Boolean = err.isDefined

  def getLong: Long       = l
  def getBoolean: Boolean = b
  def getDouble: Double   = d
  def getString: String   = s
  def getObject: AnyRef   = o

  def getValueType: ColumnType = valueType

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

  private def setValueType(vt: ColumnType) {
    err = None
    valueType = vt
  }

  def setLong(v: Long): Long = {
    l = v
    setValueType(Schema.INTEGER)
    v
  }

  def setBoolean(v: Boolean): Boolean = {
    b = v
    setValueType(Schema.BOOLEAN)
    v
  }

  def setDouble(v: Double): Double = {
    d = v
    setValueType(Schema.FLOAT)
    v
  }

  def setString(v: String): String = {
    s = v
    setValueType(Schema.STRING)
    v
  }

  def setObject[A](v: A): A = {
    o = v.asInstanceOf[AnyRef]
    if (v != null) {
      setValueType(Schema.ANY)
    } else {
      setNull
    }
    v
  }

  def setNull {
    setValueType(Schema.NIL)
  }

  def setError(e: Throwable) {
    setNull
    err = Option(e)
  }

  def setIncompatibleFormatException(message: String) {
    setError(new MessageCodecException(INVALID_DATA, message))
  }

}
