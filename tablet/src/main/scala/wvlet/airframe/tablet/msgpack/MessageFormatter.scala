package wvlet.airframe.tablet.msgpack

import org.msgpack.core.{MessagePacker, MessageUnpacker}
import org.msgpack.value.{ValueType, Variable}
import wvlet.airframe.tablet.Schema
import wvlet.airframe.tablet.Schema.ColumnType
import wvlet.surface.Surface

import scala.util.{Failure, Success, Try}

trait MessageFormatter[@specialized(Int, Long, Float, Double, Boolean) A] {
  def pack(p: MessagePacker, v: A)
  def unpack(u: MessageUnpacker, v: MessageHolder)
}

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

object LongFormatter extends MessageFormatter[Long] {
  override def pack(p: MessagePacker, v: Long): Unit = {
    p.packLong(v)
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val f  = u.getNextFormat
    val vt = f.getValueType
    vt match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.INTEGER =>
        v.setLong(u.unpackLong())
      case ValueType.STRING =>
        Try(u.unpackString.toLong) match {
          case Success(l) =>
            v.setLong(l)
          case Failure(e) =>
            v.setNull
        }
      case ValueType.BOOLEAN =>
        v.setLong(if (u.unpackBoolean()) {
          1
        } else {
          0
        })
      case ValueType.FLOAT =>
        v.setLong(u.unpackDouble().toLong)
      case other =>
        u.skipValue()
        v.setNull
    }
  }
}

object StringFormatter extends MessageFormatter[String] {
  override def pack(p: MessagePacker, v: String): Unit = {
    p.packString(v)
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    u.getNextFormat.getValueType match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.STRING =>
        v.setString(u.unpackString())
      case ValueType.INTEGER =>
        v.setString(u.unpackLong().toString)
      case ValueType.BOOLEAN =>
        v.setString(u.unpackBoolean().toString)
      case ValueType.FLOAT =>
        v.setString(u.unpackDouble().toString)
      case ValueType.MAP =>
        val m = u.unpackValue()
        v.setString(m.toJson)
      case ValueType.ARRAY =>
        val arr = v.setObject(u.unpackValue())
        v.setString(arr.toJson)
      case ValueType.BINARY =>
        val len = u.unpackBinaryHeader()
        v.setString(new String(u.readPayload(len)))
      case ValueType.EXTENSION =>
        val ext = u.unpackValue()
        v.setString(ext.toJson)
    }
  }
}

object BooleanFormatter extends MessageFormatter[Boolean] {
  override def pack(p: MessagePacker, v: Boolean): Unit = {
    p.packBoolean(v)
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    u.getNextFormat.getValueType match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.BOOLEAN =>
        v.setBoolean(u.unpackBoolean())
      case ValueType.STRING =>
        val s = u.unpackString()
        v.setBoolean(s.toBoolean)
      case ValueType.INTEGER =>
        val l = u.unpackLong()
        v.setBoolean(l != 0L)
      case other =>
        u.skipValue()
        v.setNull
    }
  }
}

object DoubleFormatter extends MessageFormatter[Double] {
  override def pack(p: MessagePacker, v: Double): Unit = {
    p.packDouble(v)
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    u.getNextFormat.getValueType match {
      case ValueType.NIL =>
        u.unpackNil()
        v.setNull
      case ValueType.FLOAT =>
        v.setDouble(u.unpackDouble())
      case ValueType.INTEGER =>
        v.setDouble(u.unpackLong().toDouble)
      case ValueType.STRING =>
        Try(u.unpackString.toDouble) match {
          case Success(d) => v.setDouble(d)
          case Failure(e) => v.setNull
        }
      case other =>
        u.skipValue()
        v.setNull
    }
  }
}

object IntArrayFormatter extends MessageFormatter[Array[Int]] {
  override def pack(p: MessagePacker, v: Array[Int]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      LongFormatter.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Int]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      LongFormatter.unpack(u, v)
      if (v.isNull) {
        // TODO report error?
        b += 0
      } else {
        val l = v.getLong
        if (l.isValidInt) {
          b += l.toInt
        } else {
          // report error?
          b += 0
        }
      }
    }
    v.setObject(b.result())
  }
}

object LongArrayFormatter extends MessageFormatter[Array[Long]] {
  override def pack(p: MessagePacker, v: Array[Long]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      LongFormatter.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Long]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      LongFormatter.unpack(u, v)
      if (v.isNull) {
        // TODO report error?
        b += 0L
      } else {
        val l = v.getLong
        b += l
      }
    }
    v.setObject(b.result())
  }
}

object FlaotArrayFormatter extends MessageFormatter[Array[Float]] {
  override def pack(p: MessagePacker, v: Array[Float]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      DoubleFormatter.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Float]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      val d = DoubleFormatter.unpack(u, v)
      if (v.isNull) {
        // report error?
        b += 0
      } else {
        b += v.getDouble.toFloat
      }
    }
    v.setObject(b.result())
  }
}

object DoubleArrayFormatter extends MessageFormatter[Array[Double]] {
  override def pack(p: MessagePacker, v: Array[Double]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      DoubleFormatter.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Double]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      val d = DoubleFormatter.unpack(u, v)
      if (v.isNull) {
        // report error?
        b += 0
      } else {
        b += v.getDouble
      }
    }
    v.setObject(b.result())
  }
}

object BooleanArrayFormatter extends MessageFormatter[Array[Boolean]] {
  override def pack(p: MessagePacker, v: Array[Boolean]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      BooleanFormatter.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[Boolean]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      BooleanFormatter.unpack(u, v)
      if (v.isNull) {
        // report error?
        b += false
      } else {
        b += true
      }
    }
    v.setObject(b.result())
  }
}

object StringArrayFormatter extends MessageFormatter[Array[String]] {
  override def pack(p: MessagePacker, v: Array[String]): Unit = {
    p.packArrayHeader(v.length)
    v.foreach { x =>
      StringFormatter.pack(p, x)
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val len = u.unpackArrayHeader()
    val b   = Array.newBuilder[String]
    b.sizeHint(len)
    (0 until len).foreach { i =>
      StringFormatter.unpack(u, v)
      if (v.isNull) {
        // skip
      } else {
        b += v.getString
      }
    }
    v.setObject(b.result())

  }
}

class ObjectFormatter[A](cl: Class[A], codecTable: Map[Class[_], MessageFormatter[_]]) {}
