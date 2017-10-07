package wvlet.airframe.tablet.obj

import org.msgpack.core.MessageUnpacker
import org.msgpack.value.ValueType
import wvlet.airframe.tablet.msgpack.{MessageCodec, MessageHolder}
import wvlet.airframe.tablet.{Column, Record, Schema, TabletWriter}
import wvlet.log.LogSupport
import wvlet.surface._
import wvlet.surface.reflect.{ReflectTypeUtil, SurfaceFactory}

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class ObjectWriter[A](surface: Surface, codec: Map[Surface, MessageCodec[_]] = Map.empty) extends TabletWriter[A] with LogSupport {

  override def write(record: Record): A = {
    val unpacker = record.unpacker
    unpackObj(unpacker, surface).asInstanceOf[A]
  }

  override def close(): Unit = {}

  private def unpack(unpacker: MessageUnpacker, objType: Surface): AnyRef = {
    objType match {
      case Alias(_, _, orig) =>
        unpack(unpacker, orig)
      case OptionSurface(cl, elemType) =>
        if (unpacker.getNextFormat.getValueType.isNilType) {
          unpacker.unpackNil()
          None
        } else {
          Some(unpack(unpacker, elemType))
        }
      case _ =>
        val f  = unpacker.getNextFormat
        val vt = f.getValueType
        val readValue =
          if (!vt.isNilType && codec.contains(objType)) {
            val m = new MessageHolder
            codec(objType).unpack(unpacker, m)
            m.getLastValue
          } else {
            vt match {
              case ValueType.NIL =>
                unpacker.unpackNil()
                Zero.zeroOf(objType)
              case ValueType.BOOLEAN =>
                java.lang.Boolean.valueOf(unpacker.unpackBoolean())
              case ValueType.INTEGER =>
                val l = unpacker.unpackLong()
                objType match {
                  case Primitive.Byte =>
                    java.lang.Byte.valueOf(l.toByte)
                  case Primitive.Short =>
                    java.lang.Short.valueOf(l.toShort)
                  case Primitive.Int =>
                    java.lang.Integer.valueOf(l.toInt)
                  case Primitive.Long =>
                    java.lang.Long.valueOf(l)
                }
              case ValueType.FLOAT =>
                java.lang.Double.valueOf(unpacker.unpackDouble())
              case ValueType.STRING =>
                unpacker.unpackString()
              case ValueType.BINARY =>
                val size = unpacker.unpackBinaryHeader()
                unpacker.readPayload(size)
              case ValueType.ARRAY =>
                objType match {
                  case g: GenericSurface if ReflectTypeUtil.isSeq(g.rawType) || ReflectTypeUtil.isJavaColleciton(g.rawType) =>
                    val b    = Seq.newBuilder[Any]
                    val size = unpacker.unpackArrayHeader()
                    (0 until size).foreach { i =>
                      b += unpack(unpacker, g.typeArgs(0))
                    }
                    b.result()
                  case _ =>
                    unpackObj(unpacker, objType)
                }
              case ValueType.MAP =>
                // TODO? support mapping key->value to obj?
                objType match {
                  case g: GenericSurface if ReflectTypeUtil.isMap(g.rawType) || ReflectTypeUtil.isJavaMap(g.rawType) =>
                    val keyType   = g.typeArgs(0)
                    val valueType = g.typeArgs(1)
                    val b         = Map.newBuilder[Any, Any]
                    val size      = unpacker.unpackMapHeader()
                    (0 until size).foreach { i =>
                      val k = unpack(unpacker, keyType)
                      val v = unpack(unpacker, valueType)
                      b += (k -> v)
                    }
                    b.result()
                  case _ =>
                    // Fallback to JSON
                    val v = unpacker.unpackValue()
                    v.toJson
                }
              case ValueType.EXTENSION =>
                warn(s"Extension type mapping is not properly supported")
                // Fallback to JSON
                unpacker.unpackValue().toJson
            }
          }
        readValue.asInstanceOf[AnyRef]
    }
  }

  private def unpackObj(unpacker: MessageUnpacker, objType: Surface): AnyRef = {
    val f = unpacker.getNextFormat
    trace(s"unpack obj ${f} -> ${objType}")
    if (codec.contains(objType)) {
      if (f.getValueType == ValueType.NIL) {
        unpacker.unpackNil()
        null
      } else {
        val m = new MessageHolder
        codec(objType).unpack(unpacker, m)
        m.getLastValue.asInstanceOf[AnyRef]
      }
    } else if (objType.isPrimitive) {
      unpack(unpacker, objType)
    } else {
      val size   = unpacker.unpackArrayHeader()
      var index  = 0
      val args   = Array.newBuilder[AnyRef]
      val params = objType.params
      while (index < size) {
        args += unpack(unpacker, params(index).surface)
        index += 1
      }
      val a = args.result()
      trace(s"${a.map(x => s"${x.getClass.getName}:${x}").mkString("\n")}")
      val r = objType.objectFactory.map(_.newInstance(a).asInstanceOf[AnyRef]).getOrElse(null)
      trace(r)
      r
    }
  }

}

object ObjectWriter {

  def createScheamOf[A: ru.TypeTag](name: String): Schema = {
    val schema = wvlet.surface.reflect.SurfaceFactory.of[A]
    val tabletColumnTypes: Seq[Column] = for ((p, i) <- schema.params.zipWithIndex) yield {
      val vt = p.surface
      val columnType: Schema.ColumnType = vt match {
        case Primitive.Byte    => Schema.INTEGER
        case Primitive.Short   => Schema.INTEGER
        case Primitive.Int     => Schema.INTEGER
        case Primitive.Long    => Schema.INTEGER
        case Primitive.Float   => Schema.FLOAT
        case Primitive.Double  => Schema.FLOAT
        case Primitive.Char    => Schema.STRING
        case Primitive.Boolean => Schema.BOOLEAN
        case Primitive.String  => Schema.STRING
        case _                 =>
          // TODO support Option, Array, Map, the other types etc.
          Schema.STRING
      }
      Column(i, p.name, columnType)
    }
    Schema(name, tabletColumnTypes)
  }

  def of[A: ru.TypeTag]: ObjectWriter[A] = new ObjectWriter(SurfaceFactory.of[A])
}
