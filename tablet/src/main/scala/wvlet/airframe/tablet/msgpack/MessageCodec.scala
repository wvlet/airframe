package wvlet.airframe.tablet.msgpack

import org.msgpack.core.{MessageFormatException, MessagePacker, MessageUnpacker}
import org.msgpack.value.Value
import wvlet.airframe.tablet.msgpack.StandardCodec.ObjectCodec
import wvlet.surface.Surface
import wvlet.surface.reflect.RuntimeSurface

import scala.util.{Failure, Success, Try}

trait MessageCodec[@specialized(Int, Long, Float, Double, Boolean, Byte) A] {
  def pack(p: MessagePacker, v: A)
  def unpack(u: MessageUnpacker, v: MessageHolder)

  def u(u: MessageUnpacker, v: MessageHolder) {
    Try(unpack(u, v)) match {
      case Success(_) =>
      // do nothing
      case Failure(e) =>
        e match {
          case me: MessageFormatException =>
          // On MessagePack related error, we cannot continue unpacking

          case e: Throwable =>
        }
    }
  }
}

trait MessageValueCodec[A] {
  def pack(v: A): Value
  def unpack(v: Value): A
}

object MessageCodec {

  import scala.reflect.runtime.{universe => ru}

  def default = new CodecFactory(StandardCodec.primitiveCodecs)

  def of[A: ru.TypeTag]: MessageCodec[A] = default.of[A]

  class CodecFactory(knownCodecs: Map[Surface, MessageCodec[_]]) {
    private var seen = Set.empty[Surface]

    def ofSurface(surface: Surface): MessageCodec[_] = {
      if (knownCodecs.contains(surface)) {
        knownCodecs(surface)
      } else if (seen.contains(surface)) {
        throw new IllegalArgumentException(s"Codec for recursive types is not supported: ${surface}")
      } else {
        seen += surface
        val codecs = for (p <- surface.params) yield {
          ofSurface(p.surface)
        }
        ObjectCodec(surface, codecs.toIndexedSeq)
      }
    }

    def of[A: ru.TypeTag]: MessageCodec[A] = {
      val surface = RuntimeSurface.of[A]
      ofSurface(surface).asInstanceOf[MessageCodec[A]]
    }
  }
}
