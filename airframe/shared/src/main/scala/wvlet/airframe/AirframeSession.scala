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

import wvlet.airframe.AirframeException.{CYCLIC_DEPENDENCY, MISSING_DEPENDENCY}
import wvlet.airframe.Binder._
import wvlet.log.LogSupport
import wvlet.surface.Surface

import scala.util.Try

/**
  *
  */
private[airframe] class AirframeSession(sessionName: Option[String], binding: Seq[Binding], val lifeCycleManager: LifeCycleManager) extends Session with LogSupport { self =>
  import scala.collection.JavaConverters._

  private lazy val bindingTable = {
    val b = Seq.newBuilder[(Surface, Binding)]
    // Add a reference to this session to allow bind[Session]
    val sessionSurface = wvlet.surface.of[Session]
    val sessionBinding = ProviderBinding(DependencyFactory(sessionSurface, Seq.empty, LazyF0(this).asInstanceOf[Any]), true, true)
    b += sessionSurface -> sessionBinding

    // Add user-defined bindings
    binding.foreach(x => b += (x.from -> x))
    b.result.toMap[Surface, Binding]
  }

  private[airframe] def getBindingOf(t: Surface) = bindingTable.get(t)
  private[airframe] def hasSingletonOf(t: Surface): Boolean = {
    singletonHolder.contains(t)
  }

  private lazy val singletonHolder: collection.mutable.Map[Surface, Any] = new ConcurrentHashMap[Surface, Any]().asScala

  def name: String = sessionName.getOrElse(f"session:${hashCode()}%x")

  // Initialize eager singleton, pre-defined instances, eager singleton providers
  private[airframe] def init {
    debug(s"[${name}] Initializing")
    binding.collect {
      case s @ SingletonBinding(from, to, eager) if eager =>
        getInstance(from)
      case ProviderBinding(factory, provideSingleton, eager) if eager =>
        getInstance(factory.from)
    }
    debug(s"[${name}] Completed the initialization")
  }

  private[airframe] def get[A](surface: Surface): A = {
    debug(s"Get dependency [${surface}]")
    getInstance(surface, List.empty).asInstanceOf[A]
  }

  private[airframe] def getSingleton[A](surface: Surface): A = {
    debug(s"Get dependency [${surface}] as singleton")
    singletonHolder.getOrElseUpdate(surface, getInstance(surface, List.empty)).asInstanceOf[A]
  }

  private[airframe] def getOrElseUpdateSingleton[A](surface: Surface, objectFactory: => A): A = {
    singletonHolder.getOrElseUpdate(surface, getOrElseUpdate(surface, objectFactory)).asInstanceOf[A]
  }

  private[airframe] def getOrElseUpdate[A](surface: Surface, objectFactory: => A): A = {
    debug(s"Get or update dependency [${surface}]")
    bindingTable.get(surface) match {
      case Some(SingletonBinding(from, to, eager)) if from != to =>
        getSingleton(to).asInstanceOf[A]
      case Some(SingletonBinding(from, to, eager)) if from == to =>
        singletonHolder.getOrElseUpdate(from, registerInjectee(from, objectFactory)).asInstanceOf[A]
      case Some(p @ ProviderBinding(factory, provideSingleton, eager)) if provideSingleton =>
        singletonHolder.get(surface) match {
          case Some(x) =>
            x.asInstanceOf[A]
          case None =>
            getInstance(surface).asInstanceOf[A]
        }
      case other =>
        register(surface, objectFactory)
    }
  }

  private def register[A](t: Surface, obj: A): A = {
    registerInjectee(t, obj).asInstanceOf[A]
  }

  private def registerInjectee(t: Surface, obj: Any): AnyRef = {
    trace(s"registerInjectee(${t}, injectee:${obj}")
    Try(lifeCycleManager.onInit(t, obj.asInstanceOf[AnyRef])).recover {
      case e: Throwable =>
        error(s"Error occurred while executing onInject(${t}, ${obj})", e)
        throw e
    }
    obj.asInstanceOf[AnyRef]
  }

  private def getInstance(t: Surface): AnyRef = {
    getInstance(t, List.empty)
  }

  private def getInstance(t: Surface, seen: List[Surface], defaultValue: Option[Any] = None): AnyRef = {
    trace(s"Search bindings for ${t}, dependencies:[${seen.mkString(" <- ")}]")
    if (seen.contains(t)) {
      error(s"Found cyclic dependencies: ${seen}")
      throw new CYCLIC_DEPENDENCY(seen.toSet)
    }
    val obj = bindingTable.get(t).map {
      case ClassBinding(from, to) =>
        trace(s"Found a class binding from ${from} to ${to}")
        getInstance(to, t :: seen)
      case sb @ SingletonBinding(from, to, eager) =>
        trace(s"Found a singleton for ${from}: ${to}")
        singletonHolder.getOrElseUpdate(from, {
          if (from == to) {
            buildInstance(to, t :: seen)
          } else {
            getInstance(to, t :: seen)
          }
        })
      case p @ ProviderBinding(factory, provideSingleton, eager) =>
        trace(s"Found a provider for ${p.from}: ${p}")
        def buildWithProvider: AnyRef = {
          val dependencies = for (d <- factory.dependencyTypes) yield {
            getOrElseUpdate(d, getInstance(d, t :: seen))
          }
          registerInjectee(p.from, factory.create(dependencies))
        }
        if (provideSingleton) {
          singletonHolder.getOrElseUpdate(p.from, buildWithProvider)
        } else {
          buildWithProvider
        }
    }

    val result = obj.orElse(defaultValue).getOrElse {
      trace(s"No binding is found for ${t}")
      buildInstance(t, t :: seen)
    }
    result.asInstanceOf[AnyRef]
  }

  /**
    * Create a new instance of the surface
    */
  private def buildInstance(surface: Surface, seen: List[Surface]): AnyRef = {
    trace(s"buildInstance ${surface}, dependencies:[${seen.mkString(" <- ")}]")
    if (surface.isPrimitive) {
      // Cannot build Primitive types
      throw MISSING_DEPENDENCY(seen)
    } else {
      surface.objectFactory match {
        case Some(factory) =>
          trace(s"Using the default constructor for injecting ${surface}")
          val args = for (p <- surface.params) yield {
            getInstance(p.surface, seen, p.getDefaultValue)
          }
          val obj = factory.newInstance(args)
          registerInjectee(surface, obj)
        case None =>
          val obj = factoryCache.get(surface) match {
            case Some(factory) =>
              trace(s"Using pre-compiled factory for ${surface}")
              factory.asInstanceOf[Session => Any](this)
            case None =>
              //buildWithReflection(t)
              warn(
                s"No binding nor the default constructor for ${surface} is found. " +
                  s"Add bind[${surface}].toXXX to your design. dependencies:[${seen.mkString(" <- ")}]")
              throw MISSING_DEPENDENCY(seen)
          }
          registerInjectee(surface, obj)
      }
    }
  }

}
