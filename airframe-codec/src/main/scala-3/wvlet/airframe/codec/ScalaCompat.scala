package wvlet.airframe.codec

import wvlet.airframe.surface.Surface

object ScalaCompat:

  trait MessageCodecBase:
    inline def of[A]: MessageCodec[A] =
      MessageCodec.ofSurface(Surface.of[A]).asInstanceOf[MessageCodec[A]]
    inline def fromJson[A](json: String): A =
      MessageCodecFactory.defaultFactory.fromJson[A](json)
    inline def toJson[A](obj: A): String =
      MessageCodecFactory.defaultFactory.toJson[A](obj)

  trait MessageCodecFactoryBase:
    self: MessageCodecFactory =>
    inline def of[A]: MessageCodec[A] =
      self.ofSurface(Surface.of[A]).asInstanceOf[MessageCodec[A]]
    inline def fromJson[A](json: String): A =
      MessageCodec.of[A].fromJson(json)
    inline def toJson[A](obj: A): String =
      MessageCodec.of[A].toJson(obj)
