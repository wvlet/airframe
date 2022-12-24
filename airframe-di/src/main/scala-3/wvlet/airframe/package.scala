/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet

import java.util.concurrent.ConcurrentHashMap

import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.language.implicitConversions
import scala.annotation.experimental
import wvlet.airframe.DISupport

/**
  */
package object airframe {

  /**
    * The entry point to create a new design beginning from a blanc design <code> import wvlet.airframe._
    *
    * val d = design.bind[X] </code>
    */
  def newDesign: Design = Design.empty

  /**
    * Create an empty design, which sends life cycle logs to debug log level
    */
  def newSilentDesign: Design = newDesign.noLifeCycleLogging

  import scala.jdk.CollectionConverters._

  // This will not be used in Scala 3, but left for the compatibility with Scala 2
  val traitFactoryCache = new ConcurrentHashMap[Surface, Session => Any].asScala
  def getOrElseUpdateTraitFactoryCache(s: Surface, factory: Session => Any): Session => Any = {
    traitFactoryCache.getOrElseUpdate(s, factory)
  }

  inline def registerTraitFactory[A]: Unit = ${ registerTraitFactoryImpl[A] }

  import scala.quoted._

  @experimental def registerTraitFactoryImpl[A](using tpe: Type[A], q: Quotes): Expr[Unit] = {
    import quotes._
    import quotes.reflect._

    val name = "$anon"
    val parents = List(TypeTree.of[Object], TypeTree.of[A], TypeTree.of[DISupport])
    val cls = Symbol.newClass(Symbol.spliceOwner, name, parents = parents.map(_.tpe), _ => List.empty[Symbol], selfType = None)
    println(s"${cls.tree.show}")
    val clasDef = ClassDef(cls, parents, body = Nil)
    val newCls = Typed(Apply(Select(New(TypeIdent(cls)), cls.primaryConstructor), Nil), TypeTree.of[A with DISupport])

    //val exampleExpr = '{ (s: Session) => new Object with DISupport { def session: Session = s } }
    //println("===========")
    //println(exampleExpr.asTerm)

    val body = Lambda(
      owner = Symbol.spliceOwner,
      tpe = MethodType(List("s"))(_ => List(TypeRepr.of[Session]), _ => TypeRepr.of[A]),
      rhsFn = (sym: Symbol, paramRefs: List[Tree]) => {
        val s = paramRefs.head.asExprOf[Session].asTerm
        val fn = newCls
        fn.changeOwner(sym)
      }
    )
    println("=------------")
    println(body.show)

    //val fn = '{ (s: Session) => ${ newCls('s) } }


    // { (s: Session) => new A with DISupport { def session = s } }

      '{
        val s = Surface.of[A]
        ()
        //{ (ss: Session) => new A with DISupport {
        //        override def session: Session = ss
        //      }}
      }
  }
}
