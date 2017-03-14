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
import wvlet.surface.Surface

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}
import scala.util.Try

/**
  * Session manages injected objects (e.g., Singleton)
  */
trait Session extends AutoCloseable {

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
  def build[A]: A = macro AirframeMacros.buildImpl[A]

  /**
    * Internal method for building an instance of type A. This method does not inject the
    * session to A at first hand.
    *
    * @tparam A
    * @return
    */
  private[airframe] def get[A:ru.TypeTag]: A

  /**
    * Internal method for building an instance of type A using a provider generated object.
    *
    * @param obj
    * @tparam A
    * @return
    */
  private[airframe] def getOrElseUpdate[A:ru.TypeTag](obj: => A): A

  private[airframe] def getSingleton[A:ru.TypeTag]: A
  private[airframe] def getOrElseUpdateSingleton[A:ru.TypeTag](obj: => A): A

  /**
    * Get the object LifeCycleManager of this session.
    *
    * @return
    */
  def lifeCycleManager: LifeCycleManager

  def start { lifeCycleManager.start }
  def shutdown { lifeCycleManager.shutdown }
  override def close() { shutdown }

  private[airframe] def getBindingOf(t:Surface) : Option[Binding]
  private[airframe] def hasSingletonOf(t:Surface) : Boolean
}

object Session extends LogSupport {

  /**
    * To provide an access to internal Session methods (e.g, get)
    *
    * @param session
    */
  implicit class SessionAccess(session: Session) {
    def get[A:ru.TypeTag]: A = session.get[A]
    def getOrElseUpdate[A:ru.TypeTag](obj: => A): A = session.getOrElseUpdate[A](obj)
    def getSingleton[A:ru.TypeTag]: A = session.getSingleton[A]
    def getOrElseUpdateSingleton[A:ru.TypeTag](obj: => A): A = session.getOrElseUpdateSingleton[A](obj)
  }

  def getSession[A:ru.TypeTag](enclosingObj: A): Option[Session] = {
    require(enclosingObj != null, "enclosingObj is null")
    findSessionAccess(enclosingObj.getClass).flatMap { access =>
      Try(access.apply(enclosingObj.asInstanceOf[AnyRef])).toOption
    }
  }

  def findSession[A:ru.TypeTag](enclosingObj: A): Session = {
    val cl = enclosingObj.getClass
    getSession(enclosingObj).getOrElse {
      error(s"No wvlet.airframe.Session is found in the scope: ${Surface.of[A]}, " +
        s"enclosing object: ${enclosingObj}")
      throw new MISSING_SESSION(Surface.of[A])
    }
  }

  private def findSessionAccess[A:ru.TypeTag](cl: Class[A]): Option[AnyRef => Session] = {
    trace(s"Checking a session for ${cl}")

    def isSessionType(c: Class[_]) = {
      classOf[wvlet.airframe.Session].isAssignableFrom(c)
    }

    // find val or def that returns wvlet.airframe.Session
    val surface = Surface.of[A]
    val methods = Surface.methodsOf[A]

    // TODO use macros to create the method caller
//    def findSessionFromMethods: Option[AnyRef => Session] =
//      methods
//      .find(x => isSessionType(x.returnType.rawType) && x.args.isEmpty)
//      .map { sessionGetter => { obj: AnyRef => sessionGetter.invoke(obj).asInstanceOf[Session] }
//      }

    // TODO use macros to create the parameter extractor
//    def findSessionFromParams: Option[AnyRef => Session] = {
//      // Find parameters
//      surface
//      .params
//      .find(p => isSessionType(p.surface.rawType))
//      .map { sessionParam => { obj: AnyRef => sessionParam.get(obj).asInstanceOf[Session] } }
//    }

    def findEmbeddedSession: Option[AnyRef => Session] = {
      // Find any embedded session
      val m = Try(cl.getDeclaredMethod("__current_session")).toOption
      m.map { m => { obj: AnyRef => m.invoke(obj).asInstanceOf[Session] }
      }
    }

//    findSessionFromMethods
//    .orElse(findSessionFromParams)
//    .orElse(findEmbeddedSession)
    findEmbeddedSession
  }

}


