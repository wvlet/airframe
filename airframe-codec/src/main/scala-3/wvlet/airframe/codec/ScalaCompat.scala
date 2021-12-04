package wvlet.airframe.codec

import wvlet.airframe.surface.Surface

object ScalaCompat {

  trait MessageCodecBase {
    inline def of[A]: MessageCodec[A]       = ${ CodecMacros.codecOf[A] }
    inline def fromJson[A](json: String): A = ${ CodecMacros.codecFromJson[A]('json) }
    inline def toJson[A](obj: A): String    = ${ CodecMacros.codecToJson[A]('obj) }
  }

  trait MessageCodecFactoryBase { self: MessageCodecFactory =>
    inline def of[A]: MessageCodec[A]       = ${ CodecMacros.factoryOf[A]('self) }
    inline def fromJson[A](json: String): A = ${ CodecMacros.factoryFromJson[A]('json) }
    inline def toJson[A](obj: A): String    = ${ CodecMacros.factoryToJson[A]('obj) }
  }

  import scala.quoted._

  private[codec] object CodecMacros {
    def codecOf[A](using t: Type[A], quotes: Quotes): Expr[MessageCodec[A]] = {
      import quotes._
      '{ MessageCodec.ofSurface(Surface.of[A]).asInstanceOf[MessageCodec[A]] }
    }
    def codecFromJson[A](json: Expr[String])(using t: Type[A], quotes: Quotes): Expr[A] = {
      import quotes._
      '{ MessageCodecFactory.defaultFactory.fromJson[A](${ json }) }
    }
    def codecToJson[A](obj: Expr[A])(using t: Type[A], quotes: Quotes): Expr[String] = {
      import quotes._
      '{ MessageCodecFactory.defaultFactory.toJson[A](${ obj }) }
    }

    def factoryOf[A](self: Expr[MessageCodecFactory])(using t: Type[A], quote: Quotes): Expr[MessageCodec[A]] = {
      import quotes._
      '{ ${ self }.ofSurface(Surface.of[A]).asInstanceOf[MessageCodec[A]] }
    }
    def factoryFromJson[A](json: Expr[String])(using t: Type[A], quotes: Quotes): Expr[A] = {
      import quotes._
      '{ MessageCodec.of[A].fromJson(${ json }) }
    }
    def factoryToJson[A](obj: Expr[A])(using t: Type[A], quotes: Quotes): Expr[String] = {
      import quotes._
      '{ MessageCodec.of[A].toJson(${ obj }) }
    }
  }

}
