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
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType}

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}
import scala.util.Try

/**
  * Context tracks the dependencies of objects and use them to instantiate objects
  */
trait Session {

  /**
    * Creates an instance of the given type A.
    *
    * @tparam A
    * @return object
    */
  def get[A: ru.WeakTypeTag]: A
  def getOrElseUpdate[A: ru.WeakTypeTag](obj: => A): A
  def build[A: ru.WeakTypeTag]: A = macro AirframeMacros.buildImpl[A]

  def register[A: ru.WeakTypeTag](obj: A): A
}

trait SessionListener {
  def afterInjection(t: ObjectType, injectee: Any)
}

object Session extends LogSupport {

  def findSessionAccess[A](cl: Class[A]): Option[AnyRef => Session] = {
    trace(s"Find session for ${cl}")

    def returnsSession(c: Class[_]) = {
      classOf[wvlet.airframe.Session].isAssignableFrom(c)
    }

    // find val or def that returns wvlet.inject.Session
    val schema = ObjectSchema(cl)

    def findSessionFromMethods: Option[AnyRef => Session] =
      schema
      .methods
      .find(x => returnsSession(x.valueType.rawType) && x.params.isEmpty)
      .map { sessionnGetter => { obj: AnyRef => sessionnGetter.invoke(obj).asInstanceOf[Session] }
      }

    def findSessionFromParams: Option[AnyRef => Session] = {
      // Find parameters
      schema
      .parameters
      .find(p => returnsSession(p.valueType.rawType))
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

  def getSession[A](enclosingObj: A): Option[Session] = {
    require(enclosingObj != null, "enclosinbObj is null")
    findSessionAccess(enclosingObj.getClass).flatMap { access =>
      Try(access.apply(enclosingObj.asInstanceOf[AnyRef])).toOption
    }
  }

  def findSession[A](enclosingObj: A): Session = {
    val cl = enclosingObj.getClass
    getSession(enclosingObj).getOrElse {
      error(s"No wvlet.airframe.Session is found in the scope: ${ObjectType.of(cl)}, enclosing object: ${enclosingObj}")
      throw new MISSING_SESSION(ObjectType.of(cl))
    }
  }

}

