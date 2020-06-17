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
package wvlet.airframe.codec

import wvlet.airframe.codec.DataType._

/**
  * MessageContext is used for passing the parsing configuration and
  * the last value read by codec.
  *
  * For efficiency, it holds several primitive type values as
  * local variables to avoid the boxing overhead.
  */
case class MessageContext(
    // For now, we have no specific configuration to add
) {
  private var dataType: DataType     = NIL
  private var value: Option[Any]     = None
  private var err: Option[Throwable] = None

  private var l: Long    = 0L
  private var b: Boolean = false
  private var d: Double  = 0.0
  private var s: String  = ""

  def isNull: Boolean             = value.isEmpty
  def hasError: Boolean           = err.isDefined
  def getError: Option[Throwable] = err

  def setNull: Unit = {
    dataType = NIL
    value = None
  }

  def setBoolean(v: Boolean): Unit = {
    setValue(BOOLEAN, v)
    b = v
  }
  def setByte(v: Byte): Unit = {
    setValue(INTEGER, v)
    l = v
  }
  def setChar(v: Char): Unit = {
    setValue(INTEGER, v)
    l = v
  }
  def setShort(v: Short): Unit = {
    setValue(INTEGER, v)
    l = v
  }
  def setInt(v: Int): Unit = {
    setValue(INTEGER, v)
    l = v
  }
  def setLong(v: Long): Unit = {
    setValue(INTEGER, v)
    l = v
  }

  def setFloat(v: Float): Unit = {
    setValue(FLOAT, v)
    d = v
  }
  def setDouble(v: Double): Unit = {
    setValue(FLOAT, v)
    d = v
  }

  def setString(v: String): Unit = {
    setValue(STRING, v)
    s = v
  }

  def setObject(v: Any): Unit = {
    if (v == null) {
      setNull
    } else {
      setValue(ANY, v)
    }
  }

  protected def setValue(dataType: DataType, v: Any): Unit = {
    this.dataType = dataType
    if (v != null) {
      value = Some(v)
    } else {
      value = None
    }
  }

  def getByte: Byte = {
    dataType match {
      case INTEGER if l.isValidByte => l.toByte
      case _                        => 0
    }
  }

  def getInt: Int = {
    dataType match {
      case INTEGER if l.isValidInt => l.toInt
      case _                       => 0
    }
  }

  def getShort: Short = {
    dataType match {
      case INTEGER if l.isValidShort => l.toShort
      case _                         => 0
    }
  }
  def getChar: Char = {
    dataType match {
      case INTEGER if l.isValidChar => l.toChar
      case _                        => 0
    }
  }
  def getLong: Long = {
    dataType match {
      case INTEGER => l
      case _       => 0
    }
  }

  def getBoolean: Boolean = {
    dataType match {
      case BOOLEAN => b
      case _       => false
    }
  }

  def getDouble: Double = {
    dataType match {
      case FLOAT => d
      case _     => 0.0
    }
  }

  def getFloat: Float = {
    dataType match {
      case FLOAT => d.toFloat
      case _     => 0.0f
    }
  }

  def getString: String = {
    dataType match {
      case STRING => s
      case _      => value.map(_.toString).getOrElse("")
    }
  }

  def getDataType: DataType = dataType

  def getLastValue: Any = value.getOrElse(null)

  def setError[A](e: Throwable): Unit = {
    setNull
    err = Option(e)
  }

  def setIncompatibleFormatException(codec: MessageCodec[_], message: String): Unit = {
    setError(new MessageCodecException(INVALID_DATA, codec, message))
  }
}
