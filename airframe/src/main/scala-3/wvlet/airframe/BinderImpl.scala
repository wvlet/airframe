package wvlet.airframe

import wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport
import wvlet.airframe.Binder._

/**
  */
private[airframe] trait BinderImpl[A] extends LogSupport { self: Binder[A] =>

  /**
    * Bind a singleton instance of B to A
    *
    * @tparam B
    */
  inline def to[B <: A]: DesignWithContext[B] = ${ BinderMacros.toSingletonOf[B]('self, false) }
  inline def toEagerSingletonOf[B <: A]: DesignWithContext[B] = ${ BinderMacros.toSingletonOf[B]('self, true) }

//  def toInstanceOf[B <: A]: DesignWithContext[B] = ???
//  def toInstanceProvider[D1](factory: D1 => A): DesignWithContext[A] = ???
//  def toInstanceProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ???
//  def toInstanceProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ???
//  def toInstanceProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ???
//  def toInstanceProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ???

  inline def toProvider[D1](factory: D1 => A): DesignWithContext[A] = ${ BinderMacros.toProvider1[D1, A]('self, 'factory, true, false) }
  inline def toProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ${ BinderMacros.toProvider2[D1, D2, A]('self, 'factory, true, false) }
  inline def toProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ${ BinderMacros.toProvider3[D1, D2, D3, A]('self, 'factory, true, false) }
  inline def toProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ${ BinderMacros.toProvider4[D1, D2, D3, D4, A]('self, 'factory, true, false) }
  inline def toProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ${ BinderMacros.toProvider5[D1, D2, D3, D4, D5, A]('self, 'factory, true, false) }

  inline def toEagerSingletonProvider[D1](factory: D1 => A): DesignWithContext[A] = ${ BinderMacros.toProvider1[D1, A]('self, 'factory, true, true) }
  inline def toEagerSingletonProvider[D1, D2](factory: (D1, D2) => A): DesignWithContext[A] = ${ BinderMacros.toProvider2[D1, D2, A]('self, 'factory, true, true) }
  inline def toEagerSingletonProvider[D1, D2, D3](factory: (D1, D2, D3) => A): DesignWithContext[A] = ${ BinderMacros.toProvider3[D1, D2, D3, A]('self, 'factory, true, true) }
  inline def toEagerSingletonProvider[D1, D2, D3, D4](factory: (D1, D2, D3, D4) => A): DesignWithContext[A] = ${ BinderMacros.toProvider4[D1, D2, D3, D4, A]('self, 'factory, true, true) }
  inline def toEagerSingletonProvider[D1, D2, D3, D4, D5](factory: (D1, D2, D3, D4, D5) => A): DesignWithContext[A] = ${ BinderMacros.toProvider5[D1, D2, D3, D4, D5, A]('self, 'factory, true, true) }

}

private[airframe] object BinderMacros {
  import scala.quoted._

  def toSingletonOf[B](binder: Expr[Binder[_]], eager: Boolean)(using Type[B], Quotes): Expr[DesignWithContext[B]] = {
    '{
      {
        val self = ${binder}
        val to = Surface.of[B]
        if(self.from == to) {
          wvlet.log.Logger("wvlet.airframe.Binder").warn("Binding to the same type is not allowed: " + to.toString)
          throw new wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY(List(to), SourceCode())
        }
        self.design.addBinding[B](SingletonBinding(self.from, to, ${Expr.apply(eager)}, self.sourceCode))
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
          DependencyFactory(self.from, Seq(null), ${factory}),
          ${Expr.apply(singleton)},
          ${Expr.apply(eager)},
          self.sourceCode)
        )
        null.asInstanceOf[DesignWithContext[A]]
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

  def toProvider3[D1, D2, D3, A](binder: Expr[Binder[_]], factory: Expr[(D1, D2, D3) => A], singleton:Boolean, eager:Boolean)
          (using Type[D1], Type[D2], Type[D3], Type[A], Quotes): Expr[DesignWithContext[A]] = {
    '{
      {
        val self = ${binder}
        val d1 = Surface.of[D1]
        val d2 = Surface.of[D2]
        val d3 = Surface.of[D3]
        self.design.addBinding[A](ProviderBinding(
          DependencyFactory(self.from, Seq(d1, d2, d3), ${factory}),
          ${Expr.apply(singleton)},
          ${Expr.apply(eager)},
          self.sourceCode)
        )
      }
    }
  }

  def toProvider4[D1, D2, D3, D4, A](binder: Expr[Binder[_]], factory: Expr[(D1, D2, D3, D4) => A], singleton:Boolean, eager:Boolean)
          (using Type[D1], Type[D2], Type[D3], Type[D4], Type[A], Quotes): Expr[DesignWithContext[A]] = {
    '{
      {
        val self = ${binder}
        val d1 = Surface.of[D1]
        val d2 = Surface.of[D2]
        val d3 = Surface.of[D3]
        val d4 = Surface.of[D4]
        self.design.addBinding[A](ProviderBinding(
          DependencyFactory(self.from, Seq(d1, d2, d3, d4), ${factory}),
          ${Expr.apply(singleton)},
          ${Expr.apply(eager)},
          self.sourceCode)
        )
      }
    }
  }

  def toProvider5[D1, D2, D3, D4, D5, A](binder: Expr[Binder[_]], factory: Expr[(D1, D2, D3, D4, D5) => A], singleton:Boolean, eager:Boolean)
          (using Type[D1], Type[D2], Type[D3], Type[D4], Type[D5], Type[A], Quotes): Expr[DesignWithContext[A]] = {
    '{
      {
        val self = ${binder}
        val d1 = Surface.of[D1]
        val d2 = Surface.of[D2]
        val d3 = Surface.of[D3]
        val d4 = Surface.of[D4]
        val d5 = Surface.of[D5]
        self.design.addBinding[A](ProviderBinding(
          DependencyFactory(self.from, Seq(d1, d2, d3, d4, d5), ${factory}),
          ${Expr.apply(singleton)},
          ${Expr.apply(eager)},
          self.sourceCode)
        )
      }
    }
  }

}
