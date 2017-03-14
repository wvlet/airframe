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
import wvlet.surface.Surface

import scala.concurrent.duration
import scala.concurrent.duration.Duration
import scala.reflect.runtime.{universe => ru}
import scala.util.{Failure, Try}

/**
  *
  */
private[airframe] class AirframeSession(sessionName:Option[String], binding: Seq[Binding],
    val lifeCycleManager: LifeCycleManager) extends Session with LogSupport { self =>
  import scala.collection.JavaConverters._

  private lazy val bindingTable = binding.map(b => b.from -> b).toMap[Surface, Binding]
  private[airframe] def getBindingOf(t:Surface) = bindingTable.get(t)
  private[airframe] def hasSingletonOf(t: Surface): Boolean = {
    singletonHolder.contains(t)
  }

  private lazy val singletonHolder: collection.mutable.Map[Surface, Any]
    = new ConcurrentHashMap[Surface, Any]().asScala

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

  private[airframe] def get[A:ru.TypeTag]: A = {
    val surface = Surface.of[A]
    debug(s"Get dependency [${surface}]")
    getInstance(surface, List.empty).asInstanceOf[A]
  }

  private[airframe] def getSingleton[A:ru.TypeTag]: A = {
    val surface = Surface.of[A]
    debug(s"Get dependency [${surface}] as singleton")
    singletonHolder.getOrElseUpdate(surface, getInstance(surface, List.empty)).asInstanceOf[A]
  }

  private[airframe] def getOrElseUpdateSingleton[A:ru.TypeTag](obj: => A): A = {
    val surface = Surface.of[A]
    singletonHolder.getOrElseUpdate(surface, getOrElseUpdate(surface, obj)).asInstanceOf[A]
  }

  private[airframe] def getOrElseUpdate[A:ru.TypeTag](obj: => A): A = {
    val surface = Surface.of[A]
    debug(s"Get or update dependency [${surface}]")
    bindingTable.get(surface) match {
      case Some(SingletonBinding(from, to, eager)) =>
        singletonHolder.getOrElseUpdate(from, registerInjectee(from, obj)).asInstanceOf[A]
      case Some(p@ProviderBinding(factory, provideSingleton, eager)) if provideSingleton =>
        singletonHolder.get(surface) match {
          case Some(x) =>
            x.asInstanceOf[A]
          case None =>
            getInstance(surface).asInstanceOf[A]
        }
      case other =>
        register(surface, obj)
    }
  }

  private def register[A:ru.TypeTag](obj: A): A = {
    register(Surface.of[A], obj)
  }
  private def register[A](t:Surface, obj: A): A = {
    registerInjectee(t, obj).asInstanceOf[A]
  }

  private def registerInjectee(t: Surface, obj: Any): AnyRef = {
    trace(s"registerInjectee(${t}, injectee:${obj}")
    Try(lifeCycleManager.onInit(t, obj.asInstanceOf[AnyRef])).recover {
      case e:Throwable =>
        error(s"Error occurred while executing onInit(${t}, ${obj})", e)
        throw e
    }
    obj.asInstanceOf[AnyRef]
  }

  private def getInstance(t: Surface): AnyRef = {
    getInstance(t, List.empty)
  }

  private def getInstance(t: Surface, stack: List[Surface]): AnyRef = {
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

  private def buildInstance(surface: Surface, stack: List[Surface]): AnyRef = {
    trace(s"buildInstance ${surface}, stack:${stack}")
    if (surface.isPrimitive) {
      // Cannot build Primitive types
      throw MISSING_DEPENDENCY(stack)
    }
    else {
      surface.objectFactory match {
        case Some(factory) =>
          trace(s"Using the default constructor for injecting ${surface}")
          val args = for (p <- surface.params) yield {
            getInstance(p.surface, stack)
          }
          val obj = factory.newInstance(args)
          registerInjectee(surface, obj)
        case None =>
          if (!(surface.rawType.isAnonymousClass || surface.rawType.isInterface)) {
            // We cannot inject Session to a class which has no default constructor
            // No binding is found for the concrete class
            throw new MISSING_DEPENDENCY(stack)
          }
          val obj = factoryCache.get(surface) match {
            case Some(factory) =>
              trace(s"Using pre-compiled factory for ${surface}")
              factory.asInstanceOf[Session => Any](this)
            case None =>
              //buildWithReflection(t)
              throw MISSING_DEPENDENCY(List(surface))
          }
          registerInjectee(surface, obj)
      }
    }
  }
//
//  private def buildWithReflection(t:Surface) : AnyRef ={
//    // When there is no constructor, generate trait
//    import scala.reflect.runtime.currentMirror
//    import scala.tools.reflect.ToolBox
//    val tb = currentMirror.mkToolBox()
//    val typeName = t.rawType.getName.replaceAll("\\$", ".")
//    try {
//      val code =
//        s"""new (wvlet.airframe.Session => Any) {
//            |  def apply(session:wvlet.airframe.Session) = {
//            |    new ${typeName} {
//            |      protected def __current_session = session
//            |    }
//            |  }
//            |}  """.stripMargin
//      trace(s"Compiling a code for embedding Session to ${t}:\n${code}")
//      val compileStart = System.currentTimeMillis()
//      val parsed = tb.parse(code)
//      val f = tb.eval(parsed).asInstanceOf[Session => Any]
//      val compileFinished = System.currentTimeMillis()
//      val compileDuration = Duration(compileFinished - compileStart, duration.MILLISECONDS)
//      trace(f"Compilation done: ${compileDuration.toMillis / 1000.0}%.2f sec.")
//      val obj = f.apply(this)
//      registerInjectee(t, obj)
//    }
//    catch {
//      case e: Throwable =>
//        error(s"Failed to inject Session to ${t}")
//        throw e
//    }
//  }

}
