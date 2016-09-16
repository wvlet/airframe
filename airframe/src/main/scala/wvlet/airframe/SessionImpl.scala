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

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import wvlet.airframe.AirframeException.{CYCLIC_DEPENDENCY, MISSING_DEPENDENCY}
import wvlet.airframe.Binder._
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType, TypeUtil}

import scala.concurrent.duration
import scala.concurrent.duration.Duration
import scala.reflect.runtime.{universe => ru}
import scala.util.{Failure, Try}

/**
  *
  */
private[airframe] class SessionImpl(sessionName:Option[String], binding: Seq[Binding],
    val lifeCycleManager: LifeCycleManager) extends Session with LogSupport { self =>
  import scala.collection.JavaConversions._

  private lazy val bindingTable = binding.map(b => b.from -> b).toMap[ObjectType, Binding]
  private[airframe] def getBindingOf(t:ObjectType) = bindingTable.get(t)
  private[airframe] def hasSingletonOf(t: ObjectType): Boolean = {
    singletonHolder.contains(t)
  }

  private lazy val singletonHolder: collection.mutable.Map[ObjectType, Any]
    = new ConcurrentHashMap[ObjectType, Any]()

  def name : String = sessionName.getOrElse(f"session:${hashCode()}%x")

  // Initialize eager singleton, pre-defined instances, eager singleton providers
  private[airframe] def init {
    debug(s"[${name}] Initializing")
    binding.collect {
      case s@SingletonBinding(from, to, eager) if eager =>
        getInstance(from)
      case ProviderBinding(factory, provideSingleton, eager) if eager =>
        getInstance(factory.from)
    }
    debug(s"[${name}] Completed the initialization")
  }

  private[airframe] def get[A](implicit ev: ru.WeakTypeTag[A]): A = {
    val tpe = ObjectType.of[A]
    debug(s"Get dependency [${ev.tpe}]")
    getInstance(tpe, List.empty).asInstanceOf[A]
  }

  private[airframe] def getSingleton[A](implicit ev: ru.WeakTypeTag[A]): A = {
    val tpe = ObjectType.of[A]
    debug(s"Get dependency [${ev.tpe}] as singleton")
    singletonHolder.getOrElseUpdate(tpe, getInstance(tpe, List.empty)).asInstanceOf[A]
  }

  private[airframe] def getOrElseUpdateSingleton[A](obj: => A)(implicit ev: ru.WeakTypeTag[A]): A = {
    val tpe = ObjectType.of[A]
    singletonHolder.getOrElseUpdate(tpe, getOrElseUpdate(obj)(ev)).asInstanceOf[A]
  }

  private[airframe] def getOrElseUpdate[A](obj: => A)(implicit ev: ru.WeakTypeTag[A]): A = {
    debug(s"Get or update dependency [${ev.tpe}]")
    val t = ObjectType.of[A]
    bindingTable.get(t) match {
      case Some(SingletonBinding(from, to, eager)) =>
        singletonHolder.getOrElseUpdate(from, registerInjectee(from, obj)).asInstanceOf[A]
      case Some(p@ProviderBinding(factory, provideSingleton, eager)) if provideSingleton =>
        getInstance(t).asInstanceOf[A]
      case other =>
        register(obj)(ev)
    }
  }

  private[airframe] def getOrElseUpdate[A](t:ObjectType, obj: => A): A = {
    debug(s"Get or update dependency [${t}]")
    bindingTable.get(t) match {
      case Some(SingletonBinding(from, to, eager)) =>
        singletonHolder.getOrElseUpdate(from, registerInjectee(from, obj)).asInstanceOf[A]
      case Some(p@ProviderBinding(factory, provideSingleton, eager)) if provideSingleton =>
        getInstance(t).asInstanceOf[A]
      case other =>
        register(t, obj)
    }
  }

  private def register[A](obj: A)(implicit ev: ru.WeakTypeTag[A]): A = {
    register(ObjectType.of[A], obj)
  }
  private def register[A](t:ObjectType, obj: A): A = {
    registerInjectee(t, obj).asInstanceOf[A]
  }

  private def registerInjectee(t: ObjectType, obj: Any): AnyRef = {
    trace(s"registerInjectee(${t}, injectee:${obj}")
    Try(lifeCycleManager.onInit(t, obj.asInstanceOf[AnyRef])).recover {
      case e:Throwable =>
        error(s"Error occurred while executing onInit(${t}, ${obj})", e)
        throw e
    }
    obj.asInstanceOf[AnyRef]
  }

  private def getInstance(t: ObjectType): AnyRef = {
    getInstance(t, List.empty)
  }

  private def getInstance(t: ObjectType, stack: List[ObjectType]): AnyRef = {
    trace(s"Search bindings of ${t}")
    if (stack.contains(t)) {
      error(s"Found cyclic dependencies: ${stack}")
      throw new CYCLIC_DEPENDENCY(stack.toSet)
    }
    val obj = bindingTable.get(t).map {
      case ClassBinding(from, to) =>
        trace(s"Found a class binding from ${from} to ${to}")
        getInstance(to, t :: stack)
      case sb@SingletonBinding(from, to, eager) =>
        trace(s"Found a singleton for ${from}: ${to}")
        singletonHolder.getOrElseUpdate(from, {
          if(from == to) {
            buildInstance(to, t :: stack)
          }
          else {
            getInstance(to, t :: stack)
          }
        })
      case p@ProviderBinding(factory, provideSingleton, eager) =>
        trace(s"Found a provider for ${p.from}: ${p}")
        def buildWithProvider : AnyRef = {
          val dependencies = for (d <- factory.dependencyTypes) yield {
            getOrElseUpdate(d, getInstance(d, t :: stack))
          }
          registerInjectee(p.from, factory.create(dependencies))
        }
        if(provideSingleton) {
          singletonHolder.getOrElseUpdate(p.from, buildWithProvider)
        }
        else {
          buildWithProvider
        }
    }

    val result = obj.getOrElse {
      trace(s"No binding is found for ${t}")
      buildInstance(t, t :: stack)
    }
    result.asInstanceOf[AnyRef]
  }

  private def buildInstance(t: ObjectType, stack: List[ObjectType]): AnyRef = {
    trace(s"buildInstance ${t}, stack:${stack}")
    val schema = ObjectSchema.of(t)
    if (t.isPrimitive || t.isTextType) {
      // Cannot build Primitive types
      throw MISSING_DEPENDENCY(stack)
    }
    else {
      schema.findConstructor match {
        case Some(ctr) =>
          val ctrString = s"$t(${ctr.params.map(p => s"${p.name}:${p.valueType}").mkString(", ")})"
          trace(s"Using the default constructor for injecting ${ctrString}")
          val args = for (p <- ctr.params) yield {
            getInstance(p.valueType, stack)
          }
          val obj = schema.constructor.newInstance(args)
          registerInjectee(t, obj)
        case None =>
          if (!(t.rawType.isAnonymousClass || t.rawType.isInterface)) {
            // We cannot inject Session to a class which has no default constructor
            // No binding is found for the concrete class
            throw new MISSING_DEPENDENCY(stack)
          }
          val obj = Design.factoryCache.get(t.rawType) match {
            case Some(factory) =>
              trace(s"Using pre-compiled factory for ${t}")
              factory.asInstanceOf[Session => Any](this)
            case None =>
              buildWithReflection(t)
          }
          registerInjectee(t, obj)
      }
    }
  }

  private def buildWithReflection(t:ObjectType) : AnyRef ={
    // When there is no constructor, generate trait
    import scala.reflect.runtime.currentMirror
    import scala.tools.reflect.ToolBox
    val tb = currentMirror.mkToolBox()
    val typeName = t.rawType.getName.replaceAll("\\$", ".")
    try {
      val code =
        s"""new (wvlet.airframe.Session => Any) {
            |  def apply(session:wvlet.airframe.Session) = {
            |    new ${typeName} {
            |      protected def __current_session = session
            |    }
            |  }
            |}  """.stripMargin
      trace(s"Compiling a code for embedding Session to ${t}:\n${code}")
      val compileStart = System.currentTimeMillis()
      val parsed = tb.parse(code)
      val f = tb.eval(parsed).asInstanceOf[Session => Any]
      val compileFinished = System.currentTimeMillis()
      val compileDuration = Duration(compileFinished - compileStart, duration.MILLISECONDS)
      trace(f"Compilation done: ${compileDuration.toMillis / 1000.0}%.2f sec.")
      val obj = f.apply(this)
      registerInjectee(t, obj)
    }
    catch {
      case e: Throwable =>
        error(s"Failed to inject Session to ${t}")
        throw e
    }
  }

}
