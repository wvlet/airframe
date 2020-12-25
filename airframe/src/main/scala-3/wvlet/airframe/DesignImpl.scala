package wvlet.airframe

import wvlet.airframe.Binder.Binding
import wvlet.airframe.surface.Surface
import wvlet.airframe.tracing.{DIStats, Tracer}
import wvlet.log.LogSupport

import Design._
import DesignOptions._
import wvlet.airframe.lifecycle.LifeCycleHookType
import AirframeDIMacros._

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
private[airframe] trait DesignImpl extends LogSupport { self: Design =>

  inline def bind[A]: Binder[A] = {
    val __surface = Surface.of[A]
    
    wvlet.airframe.getOrElseUpdateTraitFactoryCache(__surface, { (ss: Session) => 
        new DISupport { def session = ss }.asInstanceOf[Any]
    })
    //new Binder(self.this, __surface, ${sourceCode(c)})
    new Binder(this, __surface, null.asInstanceOf[SourceCode])
  }
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
