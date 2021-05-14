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

import wvlet.airframe.di.lifecycle.LifeCycleManager
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

/**
  * Session manages injected objects (e.g., Singleton)
  */
trait Session extends SessionImpl with AutoCloseable {

  /**
    * Name of the session (default: object hash code)
    */
  def name: String

  /**
    * Id of the session (= object hash id)
    */
  def sessionId: Long

  /**
    * Reference to the design used for creating this session.
    */
  def design: Design

  /**
    * Internal method for building an instance of type A. This method does not inject the
    * session to A at first hand.
    *
    * @tparam A
    * @return
    */
  def get[A](surface: Surface)(implicit sourceCode: SourceCode): A

  /**
    * Internal method for building an instance of type A, or if no binding is found, use the given trait instance factory
    *
    * @tparam A
    * @return
    */
  def getOrElse[A](surface: Surface, traitInstanceFactory: => A)(implicit sourceCode: SourceCode): A

  private[di] def createNewInstanceOf[A](surface: Surface)(implicit sourceCode: SourceCode): A

  def getInstanceOf(surface: Surface)(implicit sourceCode: SourceCode): Any

  /**
    * Create a child session with an additional design.
    * The created session has its own singleton holder and a lifecycle manager.
    *
    * - Child sessions tries to delegate the object binding to the parent (or ancestor) session if no corresponding binding is defined in the child design.
    * - If the parent and ancestors have no binding for a given type, it will creates a new object in the child session.
    * - If the parent or an ancestor session already initialized a target binding, lifecycle hooks for that binding will not be called in the child session.
    *
    * @param d Additional design for child session
    *          @param inheritParentDesignOptions if true (default) use the same design options (e.g., production mode, life cycle logging) with the parent design
    * @return
    */
  def newChildSession(d: Design = Design.empty, inheritParentDesignOptions: Boolean = true): Session

  /**
    * Create a child session with an additional design.
    * The created session shares the same singleton holder and the lifecycle manager with this session.
    */
  def newSharedChildSession(d: Design): Session

  /**
    * Create a child session and execute the body part.
    * The created session has its own singleton holder and lifecycle manager.
    *
    * After exiting the body, the child session will be closed.
    *
    * @param d Additional design for child session.
    * @param body
    * @tparam U
    * @return
    */
  def withChildSession[U](d: Design = Design.empty)(body: Session => U): U = {
    val childSession = newChildSession(d)
    try {
      childSession.start
      body(childSession)
    } finally {
      childSession.shutdown
    }
  }

  /**
    * Get the object LifeCycleManager of this session.
    *
    * @return
    */
  def lifeCycleManager: LifeCycleManager

  def start[U](body: => U): U = {
    try {
      start
      body
    } finally {
      shutdown
    }
  }

  def start: Unit = { lifeCycleManager.start }
  def shutdown: Unit = { lifeCycleManager.shutdown }
  override def close(): Unit = { shutdown }
}

object Session extends LogSupport {

  /**
    * To provide an access to internal Session methods (e.g, get)
    *
    * @param session
    */
  implicit class SessionAccess(val session: Session) extends AnyVal {
    def get[A](surface: Surface)(implicit sourceCode: SourceCode): A = session.get[A](surface)(sourceCode)
    def getOrElse[A](surface: Surface, obj: => A)(implicit sourceCode: SourceCode): A =
      session.getOrElse[A](surface, obj)(sourceCode)
    def createNewInstanceOf[A](surface: Surface)(implicit sourceCode: SourceCode): A =
      session.createNewInstanceOf[A](surface)(sourceCode)
  }
}
