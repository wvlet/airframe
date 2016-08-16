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
package wvlet.inject

import wvlet.inject.InjectionException.MISSING_SESSION
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType}

import scala.language.experimental.macros
import scala.util.Try

object Inject extends LogSupport {

  sealed trait Binding {
    def from: ObjectType
  }
  case class ClassBinding(from: ObjectType, to: ObjectType) extends Binding
  case class InstanceBinding(from: ObjectType, to: Any) extends Binding
  case class SingletonBinding(from: ObjectType, to: ObjectType, isEager: Boolean) extends Binding
  case class ProviderBinding[A](from: ObjectType, provider: ObjectType => A) extends Binding

  def findSessionAccess[A](cl: Class[A]): Option[AnyRef => Session] = {

    trace(s"Find session for ${cl}")

    def returnsSession(c: Class[_]) = {
      classOf[wvlet.inject.Session].isAssignableFrom(c)
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
      error(s"No wvlet.inject.Session is found in the scope: ${ObjectType.of(cl)}")
      throw new InjectionException(MISSING_SESSION(ObjectType.of(cl)))
    }
  }

}

import wvlet.inject.Inject._

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class Inject extends LogSupport {

  private val binding  = Seq.newBuilder[Binding]
  private val listener = Seq.newBuilder[SessionListener]

  def bind[A](implicit a: ru.TypeTag[A]): Bind = {
    bind(ObjectType.of(a.tpe))
  }
  def bind(t: ObjectType): Bind = {
    trace(s"Bind ${t.name} [${t.rawType}]")
    val b = new Bind(this, t)
    b
  }

  def addListner[A](l: SessionListener): Inject = {
    listener += l
    this
  }

  def newSession: Session = {

    // Override preceding bindings
    val originalBindings = binding.result()
    val effectiveBindings = for ((key, lst) <- originalBindings.groupBy(_.from)) yield {
      lst.last
    }
    val keyIndex: Map[ObjectType, Int] = originalBindings.map(_.from).zipWithIndex.map(x => x._1 -> x._2).toMap
    val sortedBindings = effectiveBindings.toSeq.sortBy(x => keyIndex(x.from))
    new SessionImpl(sortedBindings, listener.result())
  }

  def addBinding(b: Binding): Inject = {
    trace(s"Add binding: $b")
    binding += b
    this
  }
}






