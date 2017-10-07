package wvlet.airframe.tablet.msgpack

import java.io.File

import org.msgpack.core.{MessagePacker, MessageUnpacker}
import wvlet.surface.reflect.RuntimeSurface
import wvlet.surface.{Primitive, Surface}
import wvlet.surface

trait MessageCodec[@specialized(Int, Long, Float, Double, Boolean, Byte) A] {
  def pack(p: MessagePacker, v: A)
  def unpack(u: MessageUnpacker, v: MessageHolder)
}

object MessageCodec {

  import scala.reflect.runtime.{universe => ru}

  val primitiveCodecs: Map[Surface, MessageCodec[_]] = Map(
    Primitive.Int              -> IntCodec,
    Primitive.Long             -> LongCodec,
    Primitive.Float            -> FloatCodec,
    Primitive.Double           -> DoubleCodec,
    Primitive.Boolean          -> BooleanCodec,
    Primitive.String           -> StringCodec,
    Primitive.Byte             -> ByteCodec,
    Primitive.Short            -> ShortCodec,
    surface.of[Array[Int]]     -> IntArrayCodec,
    surface.of[Array[Long]]    -> LongArrayCodec,
    surface.of[Array[Float]]   -> FloatArrayCodec,
    surface.of[Array[Double]]  -> DoubleArrayCodec,
    surface.of[Array[Boolean]] -> BooleanArrayCodec,
    surface.of[Array[String]]  -> StringArrayCodec,
    surface.of[Array[Byte]]    -> ByteArrayCodec,
    surface.of[File]           -> FileCodec,
    // TODO CharCodec, ShortArrayCodec, CharArrayCodec
  )

  def default = new CodecFactory(primitiveCodecs)

  def of[A: ru.TypeTag]: MessageCodec[A] = default.of[A]

  class CodecFactory(knownCodecs: Map[Surface, MessageCodec[_]]) {
    private var seen = Map.empty[Surface, MessageCodec[_]]

    def ofSurface(surface: Surface): MessageCodec[_] = {
      if (knownCodecs.contains(surface)) {
        knownCodecs(surface)
      } else if (seen.contains(surface)) {
        seen(surface)
      } else {
        val codecs = for (p <- surface.params) yield {
          ofSurface(p.surface)
        }
        val newCodec = ObjectCodec(surface, codecs.toIndexedSeq)
        seen += surface -> newCodec
        newCodec
      }
    }

    def of[A: ru.TypeTag]: MessageCodec[A] = {
      val surface = RuntimeSurface.of[A]
      ofSurface(surface).asInstanceOf[MessageCodec[A]]
    }
  }

}
