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
import wvlet.obj.{ObjectSchema, ObjectType}

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}
import scala.util.Try

/**
  * Session manages injected objects (e.g., Singleton)
  */
trait Session {

  /**
    * Name of the session (default: object hash code)
    *
    * @return
    */
  def name: String

  /**
    * Build an instance of A. In general this method is necessary only when creating an entry
    * point of your application. When feasible avoid using this method so that Airframe can
    * inject objects where bind[X] is used.
    *
    * @tparam A
    * @return object
    */
  def build[A: ru.WeakTypeTag]: A = macro AirframeMacros.buildImpl[A]

  /**
    * Internal method for building an instance of type A. This method does not inject the
    * session to A at first hand.
    *
    * @tparam A
    * @return
    */
  private[airframe] def get[A: ru.WeakTypeTag]: A

  /**
    * Internal method for building an instance of type A using a provider generated object.
    *
    * @param obj
    * @tparam A
    * @return
    */
  private[airframe] def getOrElseUpdate[A: ru.WeakTypeTag](obj: => A): A

  private[airframe] def getSingleton[A: ru.WeakTypeTag]: A
  private[airframe] def getOrElseUpdateSingleton[A: ru.WeakTypeTag](obj: => A): A

  /**
    * Get the object LifeCycleManager of this session.
    *
    * @return
    */
  def lifeCycleManager: LifeCycleManager

  def start {lifeCycleManager.start}
  def shutdown {lifeCycleManager.shutdown}

  private[airframe] def getBindingOf(t:ObjectType) : Option[Binding]
  private[airframe] def hasSingletonOf(t:ObjectType) : Boolean
}

object Session extends LogSupport {

  /**
    * To provide an access to internal Session methods (e.g, get)
    *
    * @param session
    */
  implicit class SessionAccess(session: Session) {
    def get[A: ru.WeakTypeTag]: A = session.get[A]
    def getOrElseUpdate[A: ru.WeakTypeTag](obj: => A): A = session.getOrElseUpdate[A](obj)
    def getSingleton[A: ru.WeakTypeTag]: A = session.getSingleton[A]
    def getOrElseUpdateSingleton[A: ru.WeakTypeTag](obj: => A): A = session.getOrElseUpdateSingleton[A](obj)
  }

  def getSession[A](enclosingObj: A): Option[Session] = {
    require(enclosingObj != null, "enclosingObj is null")
    findSessionAccess(enclosingObj.getClass).flatMap { access =>
      Try(access.apply(enclosingObj.asInstanceOf[AnyRef])).toOption
    }
  }

  def findSession[A](enclosingObj: A): Session = {
    val cl = enclosingObj.getClass
    getSession(enclosingObj).getOrElse {
      error(s"No wvlet.airframe.Session is found in the scope: ${ObjectType.of(cl)}, " +
        s"enclosing object: ${enclosingObj}")
      throw new MISSING_SESSION(ObjectType.of(cl))
    }
  }

  private def findSessionAccess[A](cl: Class[A]): Option[AnyRef => Session] = {
    trace(s"Checking a session for ${cl}")

    def isSessionType(c: Class[_]) = {
      classOf[wvlet.airframe.Session].isAssignableFrom(c)
    }

    // find val or def that returns wvlet.airframe.Session
    val schema = ObjectSchema(cl)

    def findSessionFromMethods: Option[AnyRef => Session] =
      schema
      .allMethods
      .find(x => isSessionType(x.valueType.rawType) && x.params.isEmpty)
      .map { sessionGetter => { obj: AnyRef => sessionGetter.invoke(obj).asInstanceOf[Session] }
      }

    def findSessionFromParams: Option[AnyRef => Session] = {
      // Find parameters
      schema
      .parameters
      .find(p => isSessionType(p.valueType.rawType))
      .map { sessionParam => { obj: AnyRef => sessionParam.get(obj).asInstanceOf[Session] } }
    }

    def findEmbeddedSession: Option[AnyRef => Session] = {
      // Find any embedded session
      val m = Try(cl.getDeclaredMethod("__current_session")).toOption
      m.map { m => { obj: AnyRef => m.invoke(obj).asInstanceOf[Session] }
      }
    }

    findSessionFromMethods
    .orElse(findSessionFromParams)
    .orElse(findEmbeddedSession)
  }

}


