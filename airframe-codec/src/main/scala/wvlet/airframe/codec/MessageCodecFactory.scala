package wvlet.airframe.codec

import wvlet.airframe.codec.ScalaStandardCodec.OptionCodec
import wvlet.airframe.surface.Surface

import scala.reflect.runtime.{universe => ru}

object MessageCodecFactory {
  // Mapping (Object Surface, Seq[Object Parameter Surface]) => MessageCodec
  type ObjectCodecFactory = Function2[Surface, Seq[MessageCodec[_]], MessageCodec[_]]

  def defaultObjectCodecFactory: ObjectCodecFactory = { (surface: Surface, paramCodec: Seq[MessageCodec[_]]) =>
    ObjectCodec(surface, paramCodec.toIndexedSeq)
  }
  def objectMapCodecFactory: ObjectCodecFactory = { (surface: Surface, paramCodec: Seq[MessageCodec[_]]) =>
    ObjectMapCodec(surface, paramCodec.toIndexedSeq)
  }

  def defaultFactory: MessageCodecFactory =
    new MessageCodecFactory(
      StandardCodec.standardCodec ++
        MetricsCodec.metricsCodec ++
        Compat.platformSpecificCodecs
    )

  private[codec] def defaultFinder(
      factory: MessageCodecFactory,
      seenSet: Set[Surface]
  ): PartialFunction[Surface, MessageCodec[_]] = {
    case s if s.isOption =>
      // Option type
      val elementSurface = s.typeArgs(0)
      OptionCodec(factory.ofSurface(elementSurface, seenSet))
  }
}

import wvlet.airframe.codec.MessageCodecFactory._

/**
  *
  */
case class MessageCodecFactory(
    knownCodecs: Map[Surface, MessageCodec[_]],
    private[codec] val objectCodecFactory: MessageCodecFactory.ObjectCodecFactory =
      MessageCodecFactory.defaultObjectCodecFactory
) {
  def withCodecs(additionalCodecs: Map[Surface, MessageCodec[_]]): MessageCodecFactory = {
    this.copy(knownCodecs = knownCodecs ++ additionalCodecs)
  }
  def withObjectMapCodec: MessageCodecFactory = {
    this.copy(objectCodecFactory = MessageCodecFactory.objectMapCodecFactory)
  }
  def withObjectCodecFactory(f: ObjectCodecFactory): MessageCodecFactory = {
    this.copy(objectCodecFactory = f)
  }

  protected[this] var cache = Map.empty[Surface, MessageCodec[_]]

  private val codecFinder: CodecFinder = Compat.codecFinder

  protected[codec] def ofSurface(surface: Surface, seen: Set[Surface] = Set.empty): MessageCodec[_] = {
    // TODO Create a fast object codec with code generation (e.g., Scala macros)

    if (knownCodecs.contains(surface)) {
      knownCodecs(surface)
    } else if (cache.contains(surface)) {
      cache(surface)
    } else if (seen.contains(surface)) {
      throw new IllegalArgumentException(s"Codec for recursive types is not supported: ${surface}")
    } else {
      val seenSet = seen + surface
      //trace(s"Finding MessageCodec of ${surface.dealias}")

      val codec =
        defaultFinder(this, seenSet)
          .orElse(codecFinder.findCodec(this, seenSet))
          .orElse[Surface, MessageCodec[_]] {
            // fallback
            case other =>
              val codecs = for (p <- other.params) yield {
                ofSurface(p.surface, seenSet)
              }
              objectCodecFactory(other, codecs.toIndexedSeq)
          }
          .apply(surface.dealias)

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
