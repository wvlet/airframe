package wvlet.airframe.di

import wvlet.airframe.di.lifecycle.{AFTER_START, BEFORE_SHUTDOWN, ON_INIT, ON_INJECT, ON_SHUTDOWN, ON_START}
import wvlet.airframe.surface.Surface

/**
  * DesignWithContext[A] is a wrapper of Design class for chaining lifecycle hooks for the same type A.
  * This can be safely cast to just Design
  */
class DesignWithContext[A](
    design: Design,
    lastSurface: Surface
) extends Design(design.designOptions, design.binding, design.hooks) {
  def onInit(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(ON_INIT, lastSurface, body.asInstanceOf[Any => Unit]))
  }
  def onInject(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(ON_INJECT, lastSurface, body.asInstanceOf[Any => Unit]))
  }
  def onStart(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(ON_START, lastSurface, body.asInstanceOf[Any => Unit]))
  }
  def afterStart(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(AFTER_START, lastSurface, body.asInstanceOf[Any => Unit]))
  }
  def beforeShutdown(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(BEFORE_SHUTDOWN, lastSurface, body.asInstanceOf[Any => Unit]))
  }
  def onShutdown(body: A => Unit): DesignWithContext[A] = {
    design.withLifeCycleHook[A](LifeCycleHookDesign(ON_SHUTDOWN, lastSurface, body.asInstanceOf[Any => Unit]))
  }
}
