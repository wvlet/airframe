package wvlet.airframe.codec

import wvlet.airframe.surface.Surface

object ScalaCompat {

  trait MessageCodecBase {
    inline def of[A]: MessageCodec[A] = ${ codecOf[A] }
    def fromJson[A](json: String): A = ???
    def toJson[A](obj: A): String = ???
  }

  trait MessageCodecFactoryBase { self: MessageCodecFactory =>
    def of[A]: MessageCodec[A]    = ???
    def of(surface: Surface): MessageCodec[_] = ???
    def fromJson[A](json: String): A = ???
    def toJson[A](obj: A): String = ???
  }

  import scala.quoted._

  private[codec] def codecOf[A](using t: Type[A], quotes: Quotes): Expr[MessageCodec[A]] = {
    import quotes._
    import quotes.reflect._

    '{ MessageCodec.ofSurface(Surface.of[A]).asInstanceOf[MessageCodec[A]] }
  }


}

