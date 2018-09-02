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
import wvlet.log.LogSupport
import wvlet.surface.Surface

import scala.language.experimental.macros

case class DesignConfig(enabledLifeCycleLogging: Boolean = true, stage: Stage = Stage.DEVELOPMENT) {
  def +(other: DesignConfig): DesignConfig = {
    // configs will be overwritten
    other
  }

  def withLifeCycleLogging: DesignConfig = {
    DesignConfig(enabledLifeCycleLogging = true, stage)
  }
  def withoutLifeCycleLogging: DesignConfig = {
    DesignConfig(enabledLifeCycleLogging = false, stage)
  }

  def withProductionMode: DesignConfig = {
    DesignConfig(enabledLifeCycleLogging, Stage.PRODUCTION)
  }

  def withLazyMode: DesignConfig = {
    DesignConfig(enabledLifeCycleLogging, Stage.DEVELOPMENT)
  }
}

/**
  * Immutable airframe design
  */
case class Design(designConfig: DesignConfig, binding: Vector[Binding]) extends LogSupport {

  def add(other: Design): Design = {
    new Design(designConfig + other.designConfig, binding ++ other.binding)
  }

  def +(other: Design): Design = add(other)

  def bind[A]: Binder[A] = macro AirframeMacros.designBindImpl[A]

  def bind(t: Surface): Binder[Any] = {
    trace(s"bind($t) ${t.isAlias}")
    val b = new Binder[Any](this, t)
    b
  }

  def addBinding(b: Binding): Design = {
    debug(s"Add binding: $b")
    new Design(designConfig, binding :+ b)
  }

  def remove[A]: Design = macro AirframeMacros.designRemoveImpl[A]

  def remove(t: Surface): Design = {
    new Design(designConfig, binding.filterNot(_.from == t))
  }

  def withLifeCycleLogging: Design = {
    new Design(designConfig.withLifeCycleLogging, binding)
  }
  def withoutLifeCycleLogging: Design = {
    new Design(designConfig.withoutLifeCycleLogging, binding)
  }

  /**
    * Enable eager initialization of singletons services for production mode
    */
  def withProductionMode: Design = {
    new Design(designConfig.withProductionMode, binding)
  }

  /**
    * Do not initialize singletons for debugging
    */
  def withLazyMode: Design = {
    new Design(designConfig.withLazyMode, binding)
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
    * A helper method of creating a new session and an instance of A.
    * This will eagerly instantiate registered singletons in the design.
    *
    * This method is useful when you only need to use A as an entry point of your program.
    * After executing the body, the sesion will be closed.
    *
    * @deprecated Use Design.withProductionMode instead
    * @param body
    * @tparam A
    * @return
    */
  def buildProduction[A](body: A => Any): Any = macro AirframeMacros.buildWithProductionSession[A]

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

  /**
    * Run the code block with a new session.
    * This method will eagerly instantiate objects registered in the design.
    *
    * This method will create a new session, start it, run the given code block, and finally terminate the session after
    * the code block completion.
    *
    * @deprecated Use Design.withProductionMode instead
    */
  def withProductionSession[U](body: Session => U): U = {
    val session = this.withProductionMode.newSessionBuilder.create
    runWithSession(session)(body)
  }

  override def toString: String = {
    s"Design:\n ${binding.mkString("\n ")}"
  }
}

object Design {

  /**
    * Empty design.
    */
  val blanc: Design = new Design(DesignConfig(), Vector.empty) // Use Vector for better append performance
}
