package wvlet.airframe.di

import wvlet.airframe.di.Binder.{DependencyFactory, ProviderBinding}
import wvlet.log.LogSupport
import wvlet.airframe.surface.Surface

/**
  */
private[di] trait BinderImpl[A] extends LogSupport { self: Binder[A] =>

  /**
    * Bind a singleton instance of B to A
    *
    * @tparam B
    */
  inline def to[B <: A]: DesignWithContext[B] = ${ BinderMacros.toSingletonOf[B]('self, false) }
  inline def toEagerSingletonOf[B <: A]: DesignWithContext[B] = ${ BinderMacros.toSingletonOf[B]('self, true) }

  inline def toProvider[D1](factory: D1 => A): DesignWithContext[A] = ${ BinderMacros.toProvider1[D1, A]('self, 'factory, true, false) }
  inline def toProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ${ BinderMacros.toProvider2[D1, D2, A]('self, 'factory, true, false) }
  def toProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ???
  def toProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ???
  def toProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ???

  inline def toEagerSingletonProvider[D1](factory: D1 => A): DesignWithContext[A] = ${ BinderMacros.toProvider1[D1, A]('self, 'factory, true, true) }
  inline def toEagerSingletonProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ${ BinderMacros.toProvider2[D1, D2, A]('self, 'factory, true, true) }
  def toEagerSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ???
  def toEagerSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ???
}

private[di] object BinderMacros {
  import scala.quoted._

  def toSingletonOf[B](binder: Expr[Binder[_]], eager: Boolean)(using Type[B], Quotes): Expr[DesignWithContext[B]] = {
    '{
      {
        val self = ${binder}
        val to = Surface.of[B]
        if(self.from == to) {
          wvlet.log.Logger("wvlet.airframe.di.Binder").warn("Binding to the same type is not allowed: " + to.toString)
          throw new wvlet.airframe.di.DIException.CYCLIC_DEPENDENCY(List(to), SourceCode())
        }
        self.design.addBinding[B](wvlet.airframe.di.Binder.SingletonBinding(self.from, to, ${Expr.apply(eager)}, self.sourceCode))
     }
    }
  }

  def toProvider1[D1, A](binder: Expr[Binder[_]], factory: Expr[D1 => A], singleton:Boolean, eager:Boolean)
          (using Type[D1], Type[A], Quotes): Expr[DesignWithContext[A]] = {
    '{
      {
        val self = ${binder}
        val d1 = Surface.of[D1]
        self.design.addBinding[A](ProviderBinding(
          DependencyFactory(self.from, Seq(d1), ${factory}),
          ${Expr.apply(singleton)},
          ${Expr.apply(eager)},
          self.sourceCode)
        )
      }
    }
  }

  def toProvider2[D1, D2, A](binder: Expr[Binder[_]], factory: Expr[(D1, D2) => A], singleton:Boolean, eager:Boolean)
          (using Type[D1], Type[D2], Type[A], Quotes): Expr[DesignWithContext[A]] = {
    '{
      {
        val self = ${binder}
        val d1 = Surface.of[D1]
        val d2 = Surface.of[D2]
        self.design.addBinding[A](ProviderBinding(
          DependencyFactory(self.from, Seq(d1, d2), ${factory}),
          ${Expr.apply(singleton)},
          ${Expr.apply(eager)},
          self.sourceCode)
        )
      }
    }
  }

}
