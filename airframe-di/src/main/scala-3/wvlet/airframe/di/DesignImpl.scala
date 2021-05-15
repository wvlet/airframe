package wvlet.airframe.di

import wvlet.log.LogSupport

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
private[di] trait DesignImpl extends LogSupport {
  def bind[A]: Binder[A] = ???
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
