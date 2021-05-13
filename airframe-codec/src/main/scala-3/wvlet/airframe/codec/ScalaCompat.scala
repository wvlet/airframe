package wvlet.airframe.codec

import wvlet.airframe.surface.Surface

object ScalaCompat {

  trait MessageCodecBase {
    def of[A]: MessageCodec[A] = ???
    def fromJson[A](json: String): A = ???
    def toJson[A](obj: A): String = ???
  }

  trait MessageCodecFactoryBase { self: MessageCodecFactory =>
    def of[A]: MessageCodec[A]    = ???
    def of(surface: Surface): MessageCodec[_] = ???
    def fromJson[A](json: String): A = ???
    def toJson[A](obj: A): String = ???
  }
}
