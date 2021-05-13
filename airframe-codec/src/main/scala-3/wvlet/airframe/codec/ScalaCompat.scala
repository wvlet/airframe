package wvlet.airframe.codec

import wvlet.airframe.surface.Surface

object ScalaCompat {

  trait MessageCodecBase {
    def of[A]: MessageCodec[A] = ???
    def fromJson[A](json: String): A = ???
    def toJson[A](obj: A): String = ???
  }

  trait MessageCodecFactoryBase { self: MessageCodecFactory =>
    def of[A]: MessageCodec[A]    = ofSurface(Surface.of[A]).asInstanceOf[MessageCodec[A]]
    def of(surface: Surface): MessageCodec[_] = ofSurface(surface)
    //def ofType(tpe: ru.Type): MessageCodec[_] = ofSurface(SurfaceFactory.ofType(tpe))

    def fromJson[A](json: String): A = {
      val codec = of[A]
      codec.fromJson(json)
    }

    def toJson[A](obj: A): String = {
      val codec = of[A]
      codec.toJson(obj)
    }
  }

}
