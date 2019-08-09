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

/**
  * Design configs
  */
case class DesignOptions(enabledLifeCycleLogging: Boolean = true,
                         stage: Stage = Stage.DEVELOPMENT,
                         options: Map[String, Any] = Map.empty)
    extends Serializable {
  def +(other: DesignOptions): DesignOptions = {
    // configs will be overwritten
    new DesignOptions(other.enabledLifeCycleLogging, other.stage, defaultOptionMerger(options, other.options))
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
    new DesignOptions(enabledLifeCycleLogging = true, stage, options)
  }
  def noLifecycleLogging: DesignOptions = {
    new DesignOptions(enabledLifeCycleLogging = false, stage, options)
  }

  def withProductionMode: DesignOptions = {
    new DesignOptions(enabledLifeCycleLogging, Stage.PRODUCTION, options)
  }

  def withLazyMode: DesignOptions = {
    new DesignOptions(enabledLifeCycleLogging, Stage.DEVELOPMENT, options)
  }

  private[airframe] def withOption[A](key: String, value: A): DesignOptions = {
    new DesignOptions(enabledLifeCycleLogging, stage, options + (key -> value))
  }

  private[airframe] def noOption[A](key: String): DesignOptions = {
    new DesignOptions(enabledLifeCycleLogging, stage, options - key)
  }

  private[airframe] def getOption[A](key: String): Option[A] = {
    options.get(key).map(_.asInstanceOf[A])
  }
}

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
case class Design private[airframe] (designOptions: DesignOptions, binding: Vector[Binding]) extends LogSupport {
  private[airframe] def getDesignConfig: DesignOptions = designOptions

  /**
    * Generates a minimized design by removing overwritten bindings
    *
    * @return
    */
  def minimize: Design = {
    var seen          = Set.empty[Surface]
    var minimizedList = List.empty[Binding]

    for (b <- binding) {
      val surface = b.from
      if (seen.contains(surface)) {
        // Remove the previous binding for the same surface
        minimizedList = minimizedList.filter(_.from != surface)
      } else {
        seen += surface
      }
      minimizedList = b :: minimizedList
    }

    Design(designOptions, minimizedList.reverse.toVector)
  }

  def add(other: Design): Design = {
    new Design(designOptions + other.designOptions, binding ++ other.binding)
  }

  def +(other: Design): Design = add(other)

  def bind[A]: Binder[A] = macro AirframeMacros.designBindImpl[A]

  def bind(t: Surface)(implicit sourceCode: SourceCode): Binder[Any] = {
    trace(s"bind($t) ${t.isAlias}")
    val b = new Binder[Any](this, t, sourceCode)
    b
  }

  def addBinding(b: Binding): Design = {
    debug(s"Add a binding: $b")
    new Design(designOptions, binding :+ b)
  }

  def remove[A]: Design = macro AirframeMacros.designRemoveImpl[A]

  def remove(t: Surface): Design = {
    new Design(designOptions, binding.filterNot(_.from == t))
  }

  def withLifeCycleLogging: Design = {
    new Design(designOptions.withLifeCycleLogging, binding)
  }

  def noLifeCycleLogging: Design = {
    new Design(designOptions.noLifecycleLogging, binding)
  }

  /**
    * Enable eager initialization of singletons services for production mode
    */
  def withProductionMode: Design = {
    new Design(designOptions.withProductionMode, binding)
  }

  /**
    * Do not initialize singletons for debugging
    */
  def withLazyMode: Design = {
    new Design(designOptions.withLazyMode, binding)
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
    new Design(designOptions.withOption(key, value), binding)
  }

  private[airframe] def noOption[A](key: String): Design = {
    new Design(designOptions.noOption(key), binding)
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
  private[airframe] val blanc: Design = new Design(new DesignOptions(), Vector.empty)

  // Empty design
  def empty: Design = blanc

  // Create a new Design
  def newDesign: Design = blanc

  private[airframe] trait AdditiveDesignOption[+A] {
    private[airframe] def addAsDesignOption[A1 >: A](other: A1): A1
  }

  private[airframe] def tracerOptionKey = "tracer"
  private[airframe] def statsOptionKey  = "stats"
}
