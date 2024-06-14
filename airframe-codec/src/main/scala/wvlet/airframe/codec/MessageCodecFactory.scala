package wvlet.airframe.codec

import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

/**
  */
case class MessageCodecFactory(codecFinder: MessageCodecFinder = Compat.messageCodecFinder, mapOutput: Boolean = true)
    extends ScalaCompat.MessageCodecFactoryBase
    with LogSupport {
  private[this] var cache = Map.empty[String, MessageCodec[_]]

  def withCodecs(additionalCodecs: Map[Surface, MessageCodec[_]]): MessageCodecFactory = {
    this.copy(codecFinder = MessageCodecFinder.newCodecFinder(additionalCodecs) orElse codecFinder)
  }
  def withCodecs(additionalCodecs: PartialFunction[Surface, MessageCodec[_]]): MessageCodecFactory = {
    this.copy(codecFinder = MessageCodecFinder.newCodecFinder(additionalCodecs) orElse codecFinder)
  }

  // Generate a codec that outputs objects as Map type. This should be enabled for generating JSON data
  def withMapOutput: MessageCodecFactory = {
    if mapOutput == true then {
      this
    } else {
      this.copy(mapOutput = true)
    }
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
      if mapOutput then {
        ObjectMapCodec(surface, codecs.toIndexedSeq)
      } else {
        ObjectCodec(surface, codecs.toIndexedSeq)
      }
  }

  def of(surface: Surface): MessageCodec[_] = ofSurface(surface)

  def ofSurface(surface: Surface, seen: Set[Surface] = Set.empty): MessageCodec[_] = {
    // TODO Create a fast object codec with code generation (e.g., Scala macros)
    val surfaceName = surface.fullName
    if cache.contains(surfaceName) then {
      cache(surfaceName)
    } else if seen.contains(surface) then {
      LazyCodec(surface, this)
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

      cache += surfaceName -> codec
      codec
    }
  }
}

object MessageCodecFactory {
  val defaultFactory: MessageCodecFactory             = new MessageCodecFactory()
  def defaultFactoryForJSON: MessageCodecFactory      = defaultFactory.withMapOutput
  def defaultFactoryForMapOutput: MessageCodecFactory = defaultFactoryForJSON

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
