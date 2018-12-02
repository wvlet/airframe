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
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.language.experimental.macros

class DesignOptions(val enabledLifeCycleLogging: Boolean = true, val stage: Stage = Stage.DEVELOPMENT)
    extends Serializable {
  def +(other: DesignOptions): DesignOptions = {
    // configs will be overwritten
    new DesignOptions(other.enabledLifeCycleLogging, other.stage)
  }

  def withLifeCycleLogging: DesignOptions = {
    new DesignOptions(enabledLifeCycleLogging = true, stage)
  }
  def withoutLifeCycleLogging: DesignOptions = {
    new DesignOptions(enabledLifeCycleLogging = false, stage)
  }

  def withProductionMode: DesignOptions = {
    new DesignOptions(enabledLifeCycleLogging, Stage.PRODUCTION)
  }

  def withLazyMode: DesignOptions = {
    new DesignOptions(enabledLifeCycleLogging, Stage.DEVELOPMENT)
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[DesignOptions]

  override def equals(other: Any): Boolean = other match {
    case that: DesignOptions =>
      (that canEqual this) &&
        enabledLifeCycleLogging == that.enabledLifeCycleLogging &&
        stage == that.stage
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(enabledLifeCycleLogging, stage)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
case class Design(designOptions: DesignOptions, private[airframe] val binding: Vector[Binding]) extends LogSupport {

  private[airframe] def getDesignConfig: DesignOptions = designOptions

  def add(other: Design): Design = {
    val b          = Vector.newBuilder[Binding]
    val newBinding = other.binding.foldLeft(binding) { case (current, b) => Design.upsertBinding(current, b) }
    new Design(designOptions + other.designOptions, newBinding)
  }

  def +(other: Design): Design = add(other)

  def bind[A]: Binder[A] = macro AirframeMacros.designBindImpl[A]

  def bind(t: Surface): Binder[Any] = {
    trace(s"bind($t) ${t.isAlias}")
    val b = new Binder[Any](this, t)
    b
  }

  def addBinding(b: Binding): Design = {
    debug(s"Add a binding: $b")
    new Design(designOptions, Design.upsertBinding(binding, b))
  }

  def remove[A]: Design = macro AirframeMacros.designRemoveImpl[A]

  def remove(t: Surface): Design = {
    new Design(designOptions, binding.filterNot(_.from == t))
  }

  def withLifeCycleLogging: Design = {
    new Design(designOptions.withLifeCycleLogging, binding)
  }

  def noLifeCycleLogging: Design = {
    new Design(designOptions.withoutLifeCycleLogging, binding)
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
    * A helper method of creating a new session and an instance of A.
    * This method is useful when you only need to use A as an entry point of your program.
    * After executing the body, the sesion will be closed.
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

  private def upsertBinding(binding: Vector[Binding], b: Binding): Vector[Binding] = {
    var replaced = false
    val newBinding = binding.map { x =>
      if (x.from == b.from) {
        replaced = true
        b
      } else {
        x
      }
    }
    if (replaced) newBinding else binding :+ b
  }
}
