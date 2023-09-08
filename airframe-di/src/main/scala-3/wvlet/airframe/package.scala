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

  @deprecated("Instantiating trait is still experimental in Scala 3.3.1", "23.9.1")
  inline def registerTraitFactory[A]: Unit = {
    // registerTraitFactoryImpl[A]
  }

  import scala.quoted._

  private def shouldGenerateTrait[A](using
      tpe: Type[A],
      q: Quotes
  ): Boolean = {
    import quotes._
    import quotes.reflect._

    val t = TypeRepr.of[A]
    val a = t.typeSymbol

    // Find the public default constructor that has no arguments
    val hasPublicDefaultConstructor: Boolean = {
      val pc = a.primaryConstructor
      pc.paramSymss.size == 1 && pc.paramSymss(0).size == 0
    }

    val hasAbstractMethods: Boolean = {
      a.methodMembers.exists { x =>
        x.flags.is(Flags.Method) &&
        (x.flags.is(Flags.Abstract) || x.flags.is(Flags.Deferred))
      }
    }
    val isTaggedType = a.fullName.startsWith("wvlet.airframe.surface.tag.")
    val isSealedType = a.flags.is(Flags.Sealed)
    val isStatic     = a.flags.is(Flags.JavaStatic)
    val isLocal      = a.flags.is(Flags.Local)
    val isTrait      = a.flags.is(Flags.Trait)

    val shouldInstantiateTrait = if (!isStatic) {
      // = Non static type
      // If X is non static type (= local class or trait),
      // we need to instantiate it first in order to populate its $outer variables

      // We cannot instantiate path-dependent types
      if (a.fullName.contains("#")) {
        false
      } else {
        !hasAbstractMethods && hasPublicDefaultConstructor
      }
    } else if (a.isAbstractType) {
      // = Abstract type
      // We cannot build abstract type X that has abstract methods, so bind[X].to[ConcreteType]
      // needs to be found in the design

      // If there is no abstract methods, it might be a trait without any method
      !hasAbstractMethods
    } else {
      // We cannot instantiate any trait or class without the default constructor
      // So binding needs to be found in the Design.
      hasPublicDefaultConstructor
    }

    // Tagged type or sealed class binding should be found in Design
    val result = isTrait && !isTaggedType && !isSealedType && shouldInstantiateTrait

    // println(s"${a.flags.show}, isStatic: ${isStatic}, isAbstract: ${a.isAbstractType}, isSealed: ${isSealedType} ${a.fullName}, " +
    //  s"has pstr: ${hasPublicDefaultConstructor} is tagged: ${isTaggedType}, has abstract method: ${hasAbstractMethods}")

    result
  }

  @experimental def registerTraitFactoryImpl[A](using
      tpe: Type[A],
      q: Quotes
  ): Expr[Unit] = {
    import quotes._
    import quotes.reflect._

    if (!shouldGenerateTrait[A]) {
      '{}
    } else {
      val name    = "$anon"
      val parents = List(TypeTree.of[Object], TypeTree.of[A], TypeTree.of[DISupport])

      //  val sessionMethod = TypeRepr.of[DISupport].typeSymbol.declaredMethod("session").head.info

      def decls(cls: Symbol): List[Symbol] =
        List(Symbol.newMethod(cls, "session", MethodType(Nil)(_ => Nil, _ => TypeRepr.of[Session])))

      val cls = Symbol.newClass(Symbol.spliceOwner, name, parents = parents.map(_.tpe), decls, selfType = None)
      // println(s"${cls.tree.show}")

      val sessionMethodSym = cls.declaredMethod("session").head

      def sessionMethodDef(s: Term) = DefDef(sessionMethodSym, argss => Some(s))

      def newCls(s: Term) = {
        val clsDef = ClassDef(cls, parents, body = List(sessionMethodDef(s)))
        val newCls = Typed(Apply(Select(New(TypeIdent(cls)), cls.primaryConstructor), Nil), TypeTree.of[A])

        Block(List(clsDef), newCls)
      }

      // Generate code { (s: Session) => new A with DISupport { def sesion: Session = s } }
      val body = Lambda(
        owner = Symbol.spliceOwner,
        tpe = MethodType(List("s"))(_ => List(TypeRepr.of[Session]), _ => TypeRepr.of[A]),
        rhsFn = (sym: Symbol, paramRefs: List[Tree]) => {
          val s  = paramRefs.head.asExprOf[Session].asTerm
          val fn = newCls(s)
          fn.changeOwner(sym)
        }
      )
      // println(body.show)

      // val exampleExpr = '{ (s: Session) => new Object with DISupport { def session: Session = s } }
      // println(exampleExpr.show)

      // Register trait factory
      // { (s: Session) => new A with DISupport { def session = s } }
      '{
        wvlet.airframe.getOrElseUpdateTraitFactoryCache(
          Surface.of[A],
          ${
            body.asExprOf[Session => Any]
          }
        )
      }
    }
  }
}
