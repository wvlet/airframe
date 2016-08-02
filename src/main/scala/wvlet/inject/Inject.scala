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

import java.util.concurrent.ConcurrentHashMap

import wvlet.inject.InjectionException.CYCLIC_DEPENDENCY
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType}

import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.util.{Failure, Try}

object Inject {

  sealed trait Binding {
    def from: ObjectType
  }
  case class ClassBinding(from: ObjectType, to: ObjectType) extends Binding
  case class InstanceBinding(from: ObjectType, to: Any) extends Binding
  case class SingletonBinding(from: ObjectType, to: ObjectType, isEager: Boolean) extends Binding
  case class ProviderBinding[A](from: ObjectType, provider: ObjectType => A) extends Binding
}

import wvlet.inject.Inject._

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class Inject extends LogSupport {

  private val binding  = Seq.newBuilder[Binding]
  private val listener = Seq.newBuilder[ContextListener]

  def bind[A](implicit a: ru.TypeTag[A]): Bind = {
    bind(ObjectType.of(a.tpe))
  }
  def bind(t: ObjectType): Bind = {
    info(s"Bind ${t.name} [${t.rawType}]")
    val b = new Bind(this, t)
    b
  }

  def addListner[A](l: ContextListener): Inject = {
    listener += l
    this
  }

  def newContext: Context = {

    // Override preceding bindings
    val b = binding.result()
    val takesLastBiding = for((key, lst) <- b.groupBy(_.from)) yield {
      lst.last
    }

    new ContextImpl(takesLastBiding.toIndexedSeq, listener.result())
  }

  def addBinding(b: Binding): Inject = {
    debug(s"Add binding: $b")
    binding += b
    this
  }
}

class Bind(h: Inject, from: ObjectType) extends LogSupport {

  def to[B](implicit ev: ru.TypeTag[B]) {
    val to = ObjectType.of(ev.tpe)
    if (from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
    }
    else {
      h.addBinding(ClassBinding(from, to))
    }
  }

  def toProvider[A: ClassTag](provider: ObjectType => A) {
    h.addBinding(ProviderBinding(from, provider))
  }

  def toSingletonOf[B](implicit ev: ru.TypeTag[B]) {
    val to = ObjectType.of(ev.tpe)
    if (from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
    }
    else {
      h.addBinding(SingletonBinding(from, to, false))
    }
  }

  def toEagerSingletonOf[B](implicit ev: ru.TypeTag[B]) {
    val to = ObjectType.of(ev.tpe)
    if (from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
    }
    else {
      h.addBinding(SingletonBinding(from, to, true))
    }
  }

  def toInstance(any: Any) {
    h.addBinding(InstanceBinding(from, any))
  }

  def toSingleton {
    h.addBinding(SingletonBinding(from, from, false))
  }

  def toEagerSingleton {
    h.addBinding(SingletonBinding(from, from, true))
  }
}

import scala.reflect.runtime.{universe => ru}

/**
  * Context tracks the dependencies of objects and use them to instantiate objects
  */
trait Context {

  /**
    * Creates an instance of the given type A.
    *
    * @tparam A
    * @return object
    */
  def get[A: ru.WeakTypeTag]: A
  //def newInstance(t: ObjectType, seen: Set[ObjectType]): AnyRef
  def getOrElseUpdate[A: ru.WeakTypeTag](obj: => A): A
  def build[A: ru.WeakTypeTag]: A = macro InjectMacros.buildImpl[A]
  //protected def buildInstance(t: ObjectType, seen: Set[ObjectType]): AnyRef = macro InjectMacros.buildInstanceImpl

}

trait ContextListener {

  def afterInjection(t: ObjectType, injectee: Any)
}

private[inject] class ContextImpl(binding: Seq[Binding], listener: Seq[ContextListener]) extends wvlet.inject.Context with LogSupport {
  self =>

  import scala.collection.JavaConversions._

  private lazy val singletonHolder: collection.mutable.Map[ObjectType, Any] = new ConcurrentHashMap[ObjectType, Any]()

  // Initialize eager singleton
  binding.collect {
    case s@SingletonBinding(from, to, eager) if eager =>
      singletonHolder.getOrElseUpdate(to, buildInstance(to, Set(to)))
    case InstanceBinding(from, obj) =>
      reportToListener(from, obj)
  }

  def get[A](implicit ev: ru.WeakTypeTag[A]): A = {
    newInstance(ObjectType.of(ev.tpe), Set.empty).asInstanceOf[A]
  }

  def getOrElseUpdate[A](obj: => A)(implicit ev: ru.WeakTypeTag[A]): A = {
    val t = ObjectType.of(ev.tpe)
    val result = binding.find(_.from == t).collect {
      case SingletonBinding(from, to, eager) =>
        singletonHolder.getOrElseUpdate(to, obj)
    }
    result.getOrElse(obj).asInstanceOf[A]
  }

  private def newInstance(t: ObjectType, seen: Set[ObjectType]): AnyRef = {
    info(s"Search bindings for ${t}")
    if (seen.contains(t)) {
      error(s"Found cyclic dependencies: ${seen}")
      throw new InjectionException(CYCLIC_DEPENDENCY(seen))
    }
    val obj = binding.find(_.from == t).map {
      case ClassBinding(from, to) =>
        newInstance(to, seen + from)
      case InstanceBinding(from, obj) =>
        info(s"Pre-defined instance is found for ${from}")
        obj
      case SingletonBinding(from, to, eager) =>
        info(s"Find a singleton for ${to}")
        singletonHolder.getOrElseUpdate(to, buildInstance(to, seen + t + to))
      case b@ProviderBinding(from, provider) =>
        info(s"Use a provider to generate ${from}: ${b}")
        provider.apply(b.from)
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
        info(s"Build a new instance for ${t}")
        val obj = schema.constructor.newInstance(args).asInstanceOf[AnyRef]
        reportToListener(t, obj)
        obj
      case None =>
        // When there is no constructor, generate trait
        // TODO use Scala macros to make it efficient
        import scala.reflect.runtime.currentMirror
        import scala.tools.reflect.ToolBox
        val tb = currentMirror.mkToolBox()
        val code =
          s"""new (wvlet.inject.Context => AnyRef) {
              |  def apply(c:wvlet.inject.Context) =
              |     new ${t.rawType.getName.replaceAll("\\$", ".")} {
              |          protected def __inject_context = c
              |     }.asInstanceOf[AnyRef]
              |}  """.stripMargin
        debug(s"compile code: ${code}")
        val f = tb.eval(tb.parse(code)).asInstanceOf[Context => AnyRef]
        f.apply(this)
    }
  }

  private def reportToListener(t: ObjectType, obj: Any) {
    listener.map(l => Try(l.afterInjection(t, obj))).collect {
      case Failure(e) =>
        error(s"Error in ContextListher", e)
        throw e
    }
  }

}



