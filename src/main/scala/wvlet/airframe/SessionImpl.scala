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

import java.util.concurrent.ConcurrentHashMap

import wvlet.airframe.Bind._
import wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType}

import scala.reflect.runtime.{universe => ru}
import scala.util.{Failure, Try}

/**
  *
  */
private[airframe] class SessionImpl(binding: Seq[Binding], listener: Seq[SessionListener]) extends wvlet.airframe.Session with LogSupport {
  self =>

  import scala.collection.JavaConversions._

  private lazy val singletonHolder: collection.mutable.Map[ObjectType, Any] = new ConcurrentHashMap[ObjectType, Any]()

  // Initialize eager singleton
  binding.collect {
    case s@SingletonBinding(from, to, eager) if eager =>
      singletonHolder.getOrElseUpdate(to, buildInstance(to, Set(to)))
    case InstanceBinding(from, obj) =>
      registerInjectee(from, obj)
  }

  def get[A](implicit ev: ru.WeakTypeTag[A]): A = {
    newInstance(ObjectType.of(ev.tpe), Set.empty).asInstanceOf[A]
  }

  def getOrElseUpdate[A](obj: => A)(implicit ev: ru.WeakTypeTag[A]): A = {
    val t = ObjectType.of(ev.tpe)
    val result = binding.find(_.from == t).collect {
      case SingletonBinding(from, to, eager) =>
        singletonHolder.getOrElseUpdate(to, {
          registerInjectee(to, obj)
        })
    }
    result.getOrElse(obj).asInstanceOf[A]
  }

  private def newInstance(t: ObjectType, seen: Set[ObjectType]): AnyRef = {
    trace(s"Search bindings for ${t}")
    if (seen.contains(t)) {
      error(s"Found cyclic dependencies: ${seen}")
      throw new AirframeException(CYCLIC_DEPENDENCY(seen))
    }
    val obj = binding.find(_.from == t).map {
      case ClassBinding(from, to) =>
        newInstance(to, seen + from)
      case InstanceBinding(from, obj) =>
        trace(s"Pre-defined instance is found for ${from}")
        obj
      case SingletonBinding(from, to, eager) =>
        trace(s"Find a singleton for ${to}")
        singletonHolder.getOrElseUpdate(to, buildInstance(to, seen + t + to))
      case b@ProviderBinding(from, provider) =>
        trace(s"Use a provider to generate ${from}: ${b}")
        registerInjectee(from, provider.apply(b.from))
    }
              .getOrElse {
                buildInstance(t, seen + t)
              }
    obj.asInstanceOf[AnyRef]
  }

  private def buildInstance(t: ObjectType, seen: Set[ObjectType]): AnyRef = {
    val schema = ObjectSchema(t.rawType)
    schema.findConstructor match {
      case Some(ctr) =>
        val args = for (p <- schema.constructor.params) yield {
          newInstance(p.valueType, seen)
        }
        trace(s"Build a new instance for ${t}")
        val obj = schema.constructor.newInstance(args)
        registerInjectee(t, obj)
      case None =>
        // When there is no constructor, generate trait
        // TODO use Scala macros to make it efficient
        import scala.reflect.runtime.currentMirror
        import scala.tools.reflect.ToolBox
        val tb = currentMirror.mkToolBox()
        val code =
          s"""new (wvlet.airframe.Session => Any) {
              |  def apply(c:wvlet.airframe.Session) =
              |     new ${t.rawType.getName.replaceAll("\\$", ".")} {
              |          protected def __current_session = c
              |     }
              |}  """.stripMargin
        trace(s"Compiling a code to embed Session: ${code}")
        val f = tb.eval(tb.parse(code)).asInstanceOf[Session => Any]
        val obj = f.apply(this)
        registerInjectee(t, obj)
    }
  }

  def register[A](obj:A)(implicit ev:ru.WeakTypeTag[A]) : A = {
    registerInjectee(ObjectType.ofTypeTag(ev), obj).asInstanceOf[A]
  }

  private def registerInjectee(t: ObjectType, obj: Any) : AnyRef ={
    trace(s"Register ${t} (${t.rawType}): ${obj}")
    listener.map(l => Try(l.afterInjection(t, obj))).collect {
      case Failure(e) =>
        error(s"Error in SessionListener", e)
        throw e
    }
    obj.asInstanceOf[AnyRef]
  }

}
