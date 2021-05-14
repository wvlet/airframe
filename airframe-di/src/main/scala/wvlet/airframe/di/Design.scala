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
package wvlet.airframe.di

import wvlet.airframe.di.Binder.Binding
import wvlet.airframe.surface.Surface
import wvlet.airframe.di.tracing.{DIStats, Tracer}
import wvlet.log.LogSupport
import Design._
import DesignOptions._
import wvlet.airframe.di.lifecycle.{
  AFTER_START,
  BEFORE_SHUTDOWN,
  LifeCycleHookType,
  ON_INIT,
  ON_INJECT,
  ON_SHUTDOWN,
  ON_START
}

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
class Design(
    private[di] val designOptions: DesignOptions,
    private[di] val binding: Vector[Binding],
    private[di] val hooks: Vector[LifeCycleHookDesign]
) extends LogSupport
    with DesignImpl {
  private[di] def getDesignConfig: DesignOptions = designOptions

  /**
    * Used for casting itself as Design if returning DesignWithContext type is cumbersome
    */
  def toDesign: Design = this

  def canEqual(other: Any): Boolean = other.isInstanceOf[Design]

  override def equals(other: Any): Boolean =
    other match {
      case that: Design =>
        (that canEqual this) &&
          designOptions == that.designOptions &&
          binding == that.binding &&
          hooks == that.hooks
      case _ => false
    }

  override def hashCode(): Int = {
    val state = Seq(designOptions, binding, hooks)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  /**
    * Generates a minimized design by removing overwritten bindings
    *
    * @return
    */
  def minimize: Design = {
    var seenBindingSurface   = Set.empty[Surface]
    var minimizedBindingList = List.empty[Binding]

    // Later binding has higher precedence, so traverse bindings from the tail
    for (b <- binding.reverseIterator) {
      val surface = b.from
      if (!seenBindingSurface.contains(surface)) {
        minimizedBindingList = b :: minimizedBindingList
        seenBindingSurface += surface
      }
    }

    var seenHooks      = Set.empty[(LifeCycleHookType, Surface)]
    var minimizedHooks = List.empty[LifeCycleHookDesign]
    // Override hooks for the same surface and event type
    for (h <- hooks.reverseIterator) {
      val key: (LifeCycleHookType, Surface) = (h.lifeCycleHookType, h.surface)
      if (!seenHooks.contains(key)) {
        minimizedHooks = h :: minimizedHooks
        seenHooks += key
      }
    }

    new Design(designOptions, minimizedBindingList.reverse.toVector, minimizedHooks.toVector)
  }

  def add(other: Design): Design = {
    new Design(designOptions + other.designOptions, binding ++ other.binding, hooks ++ other.hooks)
  }

  def +(other: Design): Design = add(other)

  def bind(t: Surface)(implicit sourceCode: SourceCode): Binder[Any] = {
    trace(s"bind($t) ${t.isAlias}")
    val b = new Binder[Any](this, t, sourceCode)
    b
  }

  def addBinding[A](b: Binding): DesignWithContext[A] = {
    debug(s"Add a binding: $b")
    new DesignWithContext[A](new Design(designOptions, binding :+ b, hooks), b.from)
  }

  private[di] def withLifeCycleHook[A](hook: LifeCycleHookDesign): DesignWithContext[A] = {
    trace(s"withLifeCycleHook: ${hook}")
    new DesignWithContext[A](new Design(designOptions, binding, hooks = hooks :+ hook), hook.surface)
  }

  def remove(t: Surface): Design = {
    new Design(designOptions, binding.filterNot(_.from == t), hooks)
  }

  def withLifeCycleLogging: Design = {
    new Design(designOptions.withLifeCycleLogging, binding, hooks)
  }

  def noLifeCycleLogging: Design = {
    new Design(designOptions.noLifecycleLogging, binding, hooks)
  }

  /**
    * Do not create default instances (i.e., binding must be defined for injecting objects)
    */
  def noDefaultInstanceInjection: Design = {
    new Design(designOptions.noDefaultInstanceInjection, binding, hooks)
  }

  /**
    * Enable eager initialization of singletons services for production mode
    */
  def withProductionMode: Design = {
    new Design(designOptions.withProductionMode, binding, hooks)
  }

  /**
    * Do not initialize singletons for debugging purpose
    */
  def withLazyMode: Design = {
    new Design(designOptions.withLazyMode, binding, hooks)
  }

  /**
    * Use a custom binding tracer
    */
  def withTracer(t: Tracer): Design = {
    withOption(tracerOptionKey, t)
  }

  def noTracer: Design = {
    noOption(tracerOptionKey)
  }

  def withStats(stats: DIStats): Design = {
    withOption(statsOptionKey, stats)
  }

  def noStats: Design = {
    noOption(statsOptionKey)
  }

  private[di] def withOption[A](key: String, value: A): Design = {
    new Design(designOptions.withOption(key, value), binding, hooks)
  }

  private[di] def noOption[A](key: String): Design = {
    new Design(designOptions.noOption(key), binding, hooks)
  }

  private[di] def getTracer: Option[Tracer] = {
    designOptions.getOption[Tracer](tracerOptionKey)
  }

  private[di] def getStats: Option[DIStats] = {
    designOptions.getOption[DIStats](statsOptionKey)
  }

  /**
    * Method for configuring the session in details
    */
  def newSessionBuilder: SessionBuilder = {
    new SessionBuilder(this)
  }

  /**
    * Create a new session.
    *
    * With this method, the session will not start automatically. You need to explicitly call
    * session.start and session.shutdown to start/terminate the lifecycle of objects
    *
    * @return
    */
  def newSession: Session = {
    new SessionBuilder(this).create
  }

  private[this] def runWithSession[U](session: Session)(body: Session => U): U = {
    try {
      session.start
      body(session)
    } finally {
      session.shutdown
    }
  }

  /**
    * Run the code block with a new session.
    *
    * This method will create a new session, start it, run the given code block, and finally terminate the session after
    * the code block completion.
    */
  def withSession[U](body: Session => U): U = {
    runWithSession(newSession)(body)
  }

  override def toString: String = {
    s"Design:\n ${binding.mkString("\n ")}"
  }
}

object Design {

  /**
    * Empty design.
    * Using Vector as a binding holder for performance and serialization reason
    */
  private[di] val blanc: Design = new Design(new DesignOptions(), Vector.empty, Vector.empty)

  // Empty design
  def empty: Design = blanc

  // Create a new Design
  def newDesign: Design = blanc

  // Create a new Design without lifecycle logging
  def newSilentDesign: Design = blanc.noLifeCycleLogging

}
