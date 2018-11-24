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

import wvlet.airframe.AirframeException.MISSING_SESSION
import wvlet.airframe.Binder.Binding
import wvlet.log.LogSupport
import wvlet.airframe.surface.Surface

import scala.language.experimental.macros
import scala.util.Try

/**
  * Session manages injected objects (e.g., Singleton)
  */
trait Session extends AutoCloseable {

  /**
    * Name of the session (default: object hash code)
    */
  def name: String

  /**
    * Reference to the design used for creating this session.
    */
  def design: Design

  /**
    * Build an instance of A. In general this method is necessary only when creating an entry
    * point of your application. When feasible avoid using this method so that Airframe can
    * inject objects where bind[X] is used.
    *
    * @tparam A
    * @return object
    */
  def build[A]: A = macro AirframeMacros.buildImpl[A]

  /**
    * Internal method for building an instance of type A. This method does not inject the
    * session to A at first hand.
    *
    * @tparam A
    * @return
    */
  private[airframe] def get[A](surface: Surface): A

  /**
    * Internal method for building an instance of type A, or if no binding is found, use the given trait instance factory
    *
    * @param obj
    * @tparam A
    * @return
    */
  private[airframe] def getOrElse[A](surface: Surface, traitInstanceFactory: => A): A

  private[airframe] def createNewInstanceOf[A](surface: Surface): A
  private[airframe] def createNewInstanceOf[A](surface: Surface, traitInstanceFactory: => A): A

  def getInstanceOf(surface: Surface): Any

  /**
    * Create a child session with an additional design.
    * The created session shares the same singleton holder and lifecycle manager with this session.
    */
  def newSharedChildSession(d: Design): Session

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

  def start: Unit            = { lifeCycleManager.start }
  def shutdown: Unit         = { lifeCycleManager.shutdown }
  override def close(): Unit = { shutdown }

  private[airframe] def getBindingOf(t: Surface): Option[Binding]
  private[airframe] def isSingletonBinding(t: Surface): Boolean
}

object Session extends LogSupport {

  /**
    * To provide an access to internal Session methods (e.g, get)
    *
    * @param session
    */
  implicit class SessionAccess(session: Session) {
    def get[A](surface: Surface): A                  = session.get[A](surface)
    def getOrElse[A](surface: Surface, obj: => A): A = session.getOrElse[A](surface, obj)
  }

  def getSession(obj: Any): Option[Session] = {
    require(obj != null, "object is null")
    findSessionAccess(obj.getClass).flatMap { access =>
      Try(access.apply(obj.asInstanceOf[AnyRef])).toOption
    }
  }

  def findSession[A](enclosingObj: A): Session = {
    getSession(enclosingObj).getOrElse {
      error(
        s"No wvlet.airframe.Session is found in the scope: ${enclosingObj.getClass}, " +
          s"enclosing object: ${enclosingObj}")
      throw new MISSING_SESSION(enclosingObj.getClass)
    }
  }

  private def isSessionType(c: Class[_]) = {
    classOf[wvlet.airframe.Session].isAssignableFrom(c)
  }

  private def findSessionAccess(cl: Class[_]): Option[AnyRef => Session] = {
    trace(s"Checking a session for ${cl}")

    def findEmbeddedSession: Option[AnyRef => Session] = {
      if (classOf[SessionHolder] isAssignableFrom (cl)) {
        Some({ obj: AnyRef =>
          obj.asInstanceOf[SessionHolder].airframeSession
        })
      } else {
        None
      }
    }

    findEmbeddedSession
  }
}
