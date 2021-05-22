package wvlet.airframe.surface.reflect

import wvlet.log.LogSupport
import wvlet.airframe.surface.Surface

import scala.quoted._
import scala.tasty.inspector._

object TastySurfaceFactory extends LogSupport {

  //..inline def of[A]: Surface = ${ TastySurfaceMacros.of[A] }

  given staging.Compiler = staging.Compiler.make(getClass.getClassLoader)

  def ofClass(cl: Class[_]): Surface = {

    val surface = staging.withQuotes { (quotes: Quotes) ?=>
      import quotes.reflect._
      val tastyType = quotes.reflect.TypeRepr.typeConstructorOf(cl)
      surfaceOf(quotes)(tastyType)
    }
    surface

//    val tastyPath: Option[String] =
//      Option(cl.getProtectionDomain.getCodeSource)
//              .map { src =>
//                s"${src.getLocation.getPath}${cl.getName.replace(".", "/")}.tasty"
//              }
//

    //    val inspector = new scala.tasty.inspector.Inspector {
//      def inspect(using quotes: Quotes)(tastys: List[Tasty[quotes.type]]): Unit = {
//        import quotes.reflect._
//        val tastyType = quotes.reflect.TypeRepr.typeConstructorOf(cl)
//        surfaceOf(quotes)(tastyType)
//        info(tastyType)
//
//        for(tasty <- tastys) {
//          val projectDir = new java.io.File(".").getCanonicalPath() + java.io.File.separator // To cleanup the paths in @SourceFile
//          val tastyStr = tasty.ast.show.replace(projectDir, "")
//          info(tastyStr)
//          info(tasty.ast)
//        }
//      }
//    }
//
//    tastyPath match {
//      case Some(p) =>
//        info(p)
//        TastyInspector.inspectTastyFiles(List(p))(inspector)
//      case _ =>
//
//    }
//
  }


  def surfaceOf(quotes: Quotes)(tpe: quotes.reflect.TypeRepr): Surface = {
    import quotes._
    info(tpe)
    null.asInstanceOf[Surface]
  }

}



object TastySurfaceMacros {


//  def of[A](using quotes: Quotes, tpe: Type[A]): Expr[Surface] = {
//    '{ null.asInstanceOf[Surface] }
//  }

}
