package wvlet.airframe.surface
import scala.quoted._

object CompileTimeSurfaceFactory {

  type SurfaceMatcher = PartialFunction[Type[_], Expr[Surface]]


  def surfaceOf[A](using tpe: Type[A], quotes: Quotes): Expr[Surface] = {
    import quotes._
    import quotes.reflect._

    def fullTypeNameOf(t: Type[_]): String = {
      val tree = TypeRepr.of(using t)
      println(tree)
      println(tree.getClass)
      tree match {
        case TypeRef(typeRepr, typeStr) =>
          typeStr
        case AppliedType(typeRepr, lstType) =>
          typeRepr.toString
      //case TypeRef(prefix, typeSymbol, args) => 
//        typeSymbol.toString
        case other => 
          tree.toString
      }
    }
    val ex = Expr(classOf[Int])
    println(Term.of(ex))
    //ex.show(using Printer.TreeCode)


    val nullFactory: Expr[Surface] = '{null}
    //println(Type.show[A])
    //println(TypeTree.of(using tpe))

    def lift[T](using t:Type[T]): Type[T] = {
       t
    }

    println(fullTypeNameOf(tpe))

    tpe match {
      case '[String] => '{ Primitive.String }
      case '[Boolean] => '{ Primitive.Boolean }
      case '[Int] => '{ Primitive.Int }
      case '[Long] => '{ Primitive.Long }
      case '[Float] => '{ Primitive.Float }
      case '[Double] => '{ Primitive.Double }
      case '[Short] => '{ Primitive.Short }
      case '[Byte] => '{ Primitive.Byte }
      case '[Char] => '{ Primitive.Char }
      case '[Unit] => '{ Primitive.Unit }
      case '[Seq[elementType]] => 
      { 
          val t = implicitly[Type[elementType]]
          val tt = TypeRepr.of(using tpe)
          //val cl = ClassOfConstant(tt)
          val ts = tt.classSymbol.get
          val cl = Constant.ClassOf(tt)
          //println(cl)
          //println(expr.show)
          //val clTree = TypeApply(ref(defn.Predef_classOf.termRef), List(TypeTree(tt)))
          val e: Expr[Surface] = '{ new GenericSurface(classOf[Int]) }
          e
      }
      case _ => nullFactory
    }
  }

}
