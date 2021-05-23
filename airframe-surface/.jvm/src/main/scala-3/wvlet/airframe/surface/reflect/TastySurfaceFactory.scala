package wvlet.airframe.surface.reflect

import wvlet.log.LogSupport
import wvlet.airframe.surface.Surface
import wvlet.airframe.surface.LazySurface

import scala.quoted._
import scala.tasty.inspector._

object TastySurfaceFactory extends LogSupport {

  given staging.Compiler = staging.Compiler.make(getClass.getClassLoader)

  def ofClass(cl: Class[_]): Surface = {
    val surface = staging.withQuotes {
      (quotes: Quotes) ?=>
      import quotes.reflect._
      val tastyType = quotes.reflect.TypeRepr.typeConstructorOf(cl)
      surfaceOfImpl(quotes, tastyType.asType)
    }
    surface
  }


  private def surfaceOfImpl(quotes: Quotes, tpe: Type[_]): Surface = {
    import quotes._
    val f = new TastySurfaceFactory(using quotes)
    f.surfaceOf(tpe)
  }

}


class TastySurfaceFactory(using quotes: Quotes) extends LogSupport {
  import quotes._
  import quotes.reflect._

  def surfaceOf(tpe: Type[_]): Surface = {
    val tr = TypeRepr.of(using tpe)
    info(tr)
    surfaceOf(tr)
  }

  private val seen = scala.collection.mutable.Set[TypeRepr]()
  private val memo = scala.collection.mutable.Map[TypeRepr, Surface]()

  private type Factory = PartialFunction[TypeRepr, Surface]

  private def surfaceOf(t: TypeRepr): Surface = {
    if(seen.contains(t)) {
      if(memo.contains(t)) {
        memo(t)
      }
      else {
        LazySurface(clsOf(t), fullTypeNameOf(t))
      }
    }
    else {
      seen += t
      // For debugging
      // println(s"[${typeNameOf(t)}]\n  ${t}")
      val generator = factory.andThen { s =>
        wvlet.airframe.surface.surfaceCache.getOrElseUpdate(fullTypeNameOf(t), s)
      }
      //println(s"--- surfaceOf(${t})")
      val surface = generator(t)
      memo += (t -> surface)
      surface
    }
  }

  private def clsOf(t:TypeRepr): Class[_] = {
    ???
  }

  private def factory: Factory = {
    //taggedTypeFactory orElse
    //aliasFactory orElse
    //higherKindedTypeFactory orElse
    primitiveTypeFactory //orElse
    //arrayFactory orElse
    //optionFactory orElse
    //tupleFactory orElse
    //javaUtilFactory orElse
    //javaEnumFactory orElse
    //exisitentialTypeFactory orElse
    //genericTypeWithConstructorFactory orElse
    //genericTypeFactory
  }


  import wvlet.airframe.surface.Primitive
  private def primitiveTypeFactory: Factory = {
    case t if t =:= TypeRepr.of[String] => Primitive.String
    case t if t =:= TypeRepr.of[Boolean] => Primitive.Boolean
    case t if t =:= TypeRepr.of[Int] =>  Primitive.Int
    case t if t =:= TypeRepr.of[Long] =>  Primitive.Long
    case t if t =:= TypeRepr.of[Float] =>  Primitive.Float
    case t if t =:= TypeRepr.of[Double] => Primitive.Double
    case t if t =:= TypeRepr.of[Short] =>  Primitive.Short
    case t if t =:= TypeRepr.of[Byte] =>  Primitive.Byte
    case t if t =:= TypeRepr.of[Char] =>  Primitive.Char
    case t if t =:= TypeRepr.of[Unit] =>  Primitive.Unit
    case t if t =:= TypeRepr.of[BigInt] =>  Primitive.BigInt
    case t if t =:= TypeRepr.of[java.math.BigInteger] =>  Primitive.BigInteger
  }

  private def fullTypeNameOf(t:Type[_]): String = {
    fullTypeNameOf(TypeRepr.of(using t))
  }

  private def fullTypeNameOf(t:TypeRepr): String = {
    def sanitize(symbol:Symbol): String = {
      val fullName = symbol.fullName
      fullName.split("\\.").toList match {
        case "scala" :: "Predef$" :: tail =>
          tail.mkString(".")
        case "scala" :: "collection" :: "immutable" :: tail =>
          tail.mkString(".")
        case "scala" :: nme :: Nil =>
          nme
        case _ =>
          fullName.replaceAll("\\$", "")
      }
    }
    t match {
      case a:AppliedType if a.args.nonEmpty =>
        s"${sanitize(a.typeSymbol)}[${a.args.map(pt => fullTypeNameOf(pt.asType)).mkString(",")}]"
      case other =>
        sanitize(other.typeSymbol)
    }
  }

  private def typeNameOf(t: Type[_]): String = {
    t.typeSymbol.fullName
  }

}
