package wvlet.airframe

import wvlet.airframe.Binder.Binding
import wvlet.airframe.surface.Surface
import wvlet.airframe.tracing.{DIStats, Tracer}
import wvlet.log.LogSupport

import Design._
import DesignOptions._
import wvlet.airframe.lifecycle.LifeCycleHookType
import AirframeDIMacros._
import scala.quoted._

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
trait DesignImpl extends LogSupport { self: Design =>
  
  inline def bind[A]: Binder[A] = ${ DesignMacros.designBind[A]('self) }

  def remove[A]: Design = ???

  /**
    * A helper method of creating a new session and an instance of A.
    * This method is useful when you only need to use A as an entry point of your program.
    * After executing the body, the sesion will be closed.
    *
    * @param body
    * @tparam A
    * @return
    */
  def build[A](body: A => Any): Any = ???

  /**
    * Execute a given code block by building A using this design, and return B
    */
  def run[A, B](body: A => B): B = ???
}


private[airframe] object DesignMacros {
  def shouldGenerateTraitOf[A](using quotes:Quotes, tpe: Type[A]): Boolean = {
    import quotes.reflect._
    val t = TypeRepr.of(using tpe)
    val ts = t.typeSymbol

    // Has a publid default constructor that can be called without any argument?
    val hasPublicDefaultConstructor = {
      val pc = ts.primaryConstructor
      pc.paramSymss.size == 1 && pc.paramSymss(0).size == 0
    }
    val hasAbstractMethods = {
      ts.memberMethods.exists(m => m.flags.is(Flags.Abstract))
    }
    val isTaggedType = ts.fullName.startsWith("wvlet.airframe.surface.tag.")
    val isSealedType = ts.flags.is(Flags.Sealed)
    val shouldInstantiateTrait = if(!ts.flags.is(Flags.Static)) {
      // = Non static type
      // If X is non static type (= local class or trait),
      // we need to instantiate it first in order to populate its $outer variables

      // We cannot instantiate path-dependent types
      if(ts.fullName.contains("#")) {
        false
      } 
      else {
        hasPublicDefaultConstructor
      }
    } else if (ts.flags.is(Flags.Abstract)) {
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
    !isTaggedType && !isSealedType && shouldInstantiateTrait
  }

  def registerTraitFactoryOf[A](using quotes: Quotes, tpe: Type[A]): Expr[Surface] = {
    if(shouldGenerateTraitOf[A]) {
      '{
        val __surface = Surface.of[A]
        wvlet.airframe.getOrElseUpdateTraitFactoryCache(__surface, { (ss: Session) => 
          new DISupport { def session = ss }.asInstanceOf[Any]
        })
        __surface
      }
    }
    else {
      '{ Surface.of[A] }     
    }
  }


  def designBind[A](design:Expr[Design])(using quotes:Quotes, tpe: Type[A]): Expr[Binder[A]] = {
    '{
        val __surface = ${registerTraitFactoryOf[A]}
        new Binder(${design}, __surface, SourceCode()).asInstanceOf[Binder[A]]
     }
  }
}