package wvlet.airframe.codec

import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}

/**
  */
case class MessageCodecFactory(codecFinder: MessageCodecFinder = Compat.messageCodecFinder, mapOutput: Boolean = false)
    extends LogSupport {
  private[this] var cache = Map.empty[Surface, MessageCodec[_]]

  def withCodecs(additionalCodecs: Map[Surface, MessageCodec[_]]): MessageCodecFactory = {
    this.copy(codecFinder = codecFinder orElse MessageCodecFinder.newCodecFinder(additionalCodecs))
  }

  // Generate a codec that outputs objects as Map type. This should be enabled for generating JSON data
  def withMapOutput: MessageCodecFactory = {
    this.copy(mapOutput = true)
  }
  def noMapOutput: MessageCodecFactory = {
    this.copy(mapOutput = false)
  }

  @deprecated(message = "use withMapOutput ", since = "19.11.0")
  def withObjectMapCodec: MessageCodecFactory = withMapOutput

  def orElse(other: MessageCodecFactory): MessageCodecFactory = {
    this.copy(codecFinder = codecFinder.orElse(other.codecFinder))
  }

  private def generateObjectSurface(seenSet: Set[Surface]): PartialFunction[Surface, MessageCodec[_]] = {
    case surface: Surface =>
      val codecs = for (p <- surface.params) yield {
        ofSurface(p.surface, seenSet)
      }
      if (mapOutput) {
        ObjectMapCodec(surface, codecs.toIndexedSeq)
      } else {
        ObjectCodec(surface, codecs.toIndexedSeq)
      }
  }

  protected[codec] def ofSurface(surface: Surface, seen: Set[Surface] = Set.empty): MessageCodec[_] = {
    // TODO Create a fast object codec with code generation (e.g., Scala macros)

    if (cache.contains(surface)) {
      cache(surface)
    } else if (seen.contains(surface)) {
      throw new IllegalArgumentException(s"Codec for recursive types is not supported: ${surface}")
    } else {
      val seenSet = seen + surface

      val codec =
        codecFinder
          .findCodec(this, seenSet)
          .orElse {
            // fallback
            generateObjectSurface(seenSet)
          }
          .apply(surface)

      cache += surface -> codec
      codec
    }
  }

  def of[A: ru.TypeTag]: MessageCodec[A]    = ofSurface(Surface.of[A]).asInstanceOf[MessageCodec[A]]
  def of(surface: Surface): MessageCodec[_] = ofSurface(surface)
  //def ofType(tpe: ru.Type): MessageCodec[_] = ofSurface(SurfaceFactory.ofType(tpe))

  def fromJson[A: ru.TypeTag](json: String): A = {
    val codec = of[A]
    codec.fromJson(json)
  }

  def toJson[A: ru.TypeTag](obj: A): String = {
    val codec = of[A]
    codec.toJson(obj)
  }
}

object MessageCodecFactory {
  val defaultFactory: MessageCodecFactory        = new MessageCodecFactory()
  def defaultFactoryForJSON: MessageCodecFactory = defaultFactory.withMapOutput

  /**
    * Create a custom MessageCodecFactory from a partial mapping
    */
  def newFactory(pf: PartialFunction[Surface, MessageCodec[_]]): MessageCodecFactory = {
    new MessageCodecFactory(codecFinder = new MessageCodecFinder {
      override def findCodec(
          factory: MessageCodecFactory,
          seenSet: Set[Surface]
      ): PartialFunction[Surface, MessageCodec[_]] = {
        pf
      }
    })
  }
}
