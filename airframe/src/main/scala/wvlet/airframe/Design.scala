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
package wvlet.airframe

import wvlet.airframe.Binder.Binding
import wvlet.airframe.Design.AdditiveDesignOption
import wvlet.airframe.surface.Surface
import wvlet.airframe.tracing.{DIStats, Tracer}
import wvlet.log.LogSupport

import scala.language.experimental.macros
import Design._
import wvlet.airframe.lifecycle.LifeCycleHookType

/**
  * Design configs
  */
case class DesignOptions(
    enabledLifeCycleLogging: Option[Boolean] = None,
    stage: Option[Stage] = None,
    defaultInstanceInjection: Option[Boolean] = None,
    options: Map[String, Any] = Map.empty
) extends Serializable {
  def +(other: DesignOptions): DesignOptions = {
    // configs will be overwritten
    new DesignOptions(
      other.enabledLifeCycleLogging.orElse(this.enabledLifeCycleLogging),
      other.stage.orElse(this.stage),
      other.defaultInstanceInjection.orElse(this.defaultInstanceInjection),
      defaultOptionMerger(options, other.options)
    )
  }

  private def defaultOptionMerger(a: Map[String, Any], b: Map[String, Any]): Map[String, Any] = {
    a.foldLeft(b) { (m, keyValue) =>
      val (key, value) = keyValue
      (m.get(key), value) match {
        case (Some(v1: AdditiveDesignOption[_]), v2: AdditiveDesignOption[_]) =>
          m + (key -> v1.addAsDesignOption(v2))
        case _ =>
          m + keyValue
      }
    }
  }

  def withLifeCycleLogging: DesignOptions = {
    this.copy(enabledLifeCycleLogging = Some(true))
  }

  def noLifecycleLogging: DesignOptions = {
    this.copy(enabledLifeCycleLogging = Some(false))
  }

  def withProductionMode: DesignOptions = {
    this.copy(stage = Some(Stage.PRODUCTION))
  }

  def withLazyMode: DesignOptions = {
    this.copy(stage = Some(Stage.DEVELOPMENT))
  }

  def noDefaultInstanceInjection: DesignOptions = {
    this.copy(defaultInstanceInjection = Some(false))
  }

  private[airframe] def withOption[A](key: String, value: A): DesignOptions = {
    this.copy(options = this.options + (key -> value))
  }

  private[airframe] def noOption[A](key: String): DesignOptions = {
    this.copy(options = this.options - key)
  }

  private[airframe] def getOption[A](key: String): Option[A] = {
    options.get(key).map(_.asInstanceOf[A])
  }
}

case class LifeCycleHookDesign(lifeCycleHookType: LifeCycleHookType, surface: Surface, hook: Any => Unit) {
  // Override toString to protect calling the hook accidentally
  override def toString: String = s"LifeCycleHookDesign[${lifeCycleHookType}](${surface})"
}

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
class Design private[airframe] (
    private[airframe] val designOptions: DesignOptions,
    private[airframe] val binding: Vector[Binding],
    private[airframe] val hooks: Vector[LifeCycleHookDesign]
) extends LogSupport {
  private[airframe] def getDesignConfig: DesignOptions = designOptions

  /**
    * Used for casting itself as Design if returning DesignWithContext type is cumbersome
    */
  def toDesign: Design = this

  def canEqual(other: Any): Boolean = other.isInstanceOf[Design]

  override def equals(other: Any): Boolean = other match {
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
    var seenBindingSurrace   = Set.empty[Surface]
    var minimizedBindingList = List.empty[Binding]

    // Later binding has higher precedence, so traverse bindings from the tail
    for (b <- binding.reverseIterator) {
      val surface = b.from
      if (!seenBindingSurrace.contains(surface)) {
        minimizedBindingList = b :: minimizedBindingList
        seenBindingSurrace += surface
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

  def bind[A]: Binder[A] = macro AirframeMacros.designBindImpl[A]

  def bind(t: Surface)(implicit sourceCode: SourceCode): Binder[Any] = {
    trace(s"bind($t) ${t.isAlias}")
    val b = new Binder[Any](this, t, sourceCode)
    b
  }

  def addBinding[A](b: Binding): DesignWithContext[A] = {
    debug(s"Add a binding: $b")
    new DesignWithContext[A](new Design(designOptions, binding :+ b, hooks), b.from)
  }

  private[airframe] def withLifeCycleHook[A](hook: LifeCycleHookDesign): DesignWithContext[A] = {
    trace(s"withLifeCycleHook: ${hook}")
    new DesignWithContext[A](new Design(designOptions, binding, hooks = hooks :+ hook), hook.surface)
  }

  def remove[A]: Design = macro AirframeMacros.designRemoveImpl[A]

  def remove(t: Surface): Design = {
    new Design(designOptions, binding.filterNot(_.from == t), hooks)
  }

  def withLifeCycleLogging: Design = {
    new Design(designOptions.withLifeCycleLogging, binding, hooks)
  }

  def noLifeCycleLogging: Design = {
    new Design(designOptions.noLifecycleLogging, binding, hooks)
  }

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
    * Do not initialize singletons for debugging
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

  private[airframe] def withOption[A](key: String, value: A): Design = {
    new Design(designOptions.withOption(key, value), binding, hooks)
  }

  private[airframe] def noOption[A](key: String): Design = {
    new Design(designOptions.noOption(key), binding, hooks)
  }

  private[airframe] def getTracer: Option[Tracer] = {
    designOptions.getOption[Tracer](tracerOptionKey)
  }

  private[airframe] def getStats: Option[DIStats] = {
    designOptions.getOption[DIStats](statsOptionKey)
  }

  /**
    * A helper method of creating a new session and an instance of A.
    * This method is useful when you only need to use A as an entry point of your program.
    * After executing the body, the sesion will be closed.
    *
    * @param body
    * @tparam A
    * @return
    */
  def build[A](body: A => Any): Any = macro AirframeMacros.buildWithSession[A]

  /**
    * Execute a given code block by building A using this design, and return B
    */
  def run[A, B](body: A => B): B = macro AirframeMacros.runWithSession[A, B]

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
  private[airframe] val blanc: Design = new Design(new DesignOptions(), Vector.empty, Vector.empty)

  // Empty design
  def empty: Design = blanc

  // Create a new Design
  def newDesign: Design = blanc

  // Create a new Design without lifecycle logging
  def newSilentDesign: Design = blanc.noLifeCycleLogging

  private[airframe] trait AdditiveDesignOption[+A] {
    private[airframe] def addAsDesignOption[A1 >: A](other: A1): A1
  }

  private[airframe] def tracerOptionKey = "tracer"
  private[airframe] def statsOptionKey  = "stats"
}
