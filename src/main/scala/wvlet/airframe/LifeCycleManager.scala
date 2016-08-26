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

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.{PostConstruct, PreDestroy}

import wvlet.airframe.Binder.{ProviderBinding, SingletonBinding}
import wvlet.log.{LogSupport, Logger}
import wvlet.obj.{ObjectMethod, ObjectSchema, ObjectType}

sealed trait LifeCycleStage
case object INIT extends LifeCycleStage
case object STARTING extends LifeCycleStage
case object STARTED extends LifeCycleStage
case object STOPPING extends LifeCycleStage
case object STOPPED extends LifeCycleStage

/**
  * LifeCycleManager manages the life cycle of objects within a Session
  */
class LifeCycleManager(eventHandler:LifeCycleEventHandler) extends LogSupport {
  self =>

  private val state = new AtomicReference[LifeCycleStage](INIT)
  def currentState: LifeCycleStage = state.get()

  private[airframe] def onInit(t:ObjectType, injectee:AnyRef) {
    eventHandler.onInit(this, t, injectee)
  }

  private var session : Session = _
  private[airframe] def setSession(s:Session) {
    session = s
  }

  def sessionName : String = session.name

  def start {
    if (!state.compareAndSet(INIT, STARTING)) {
      throw new IllegalStateException(s"LifeCycle is already starting")
    }

    eventHandler.beforeStart(this)
    // Run start hooks in the registration order
    state.set(STARTED)
    eventHandler.afterStart(this)
  }

  def shutdown {
    if (state.compareAndSet(STARTED, STOPPING) || state.compareAndSet(INIT, STOPPING)
        || state.compareAndSet(STARTING, STOPPING)) {
      eventHandler.beforeShutdown(this)
      // Run shutdown hooks in the reverse registration order
      state.set(STOPPED)
      eventHandler.afterShutdown(this)
    }
  }

  def addInitHook(h: LifeCycleHook) {
    debug(s"Add init hook: ${h}")
    // Immediately execute the init hook
    h.execute
  }

  private var startHook    = Vector.empty[LifeCycleHook]
  private var shutdownHook = Vector.empty[LifeCycleHook]

  def startHooks: Seq[LifeCycleHook] = startHook
  def shutdownHooks: Seq[LifeCycleHook] = shutdownHook

  def addStartHook(h: LifeCycleHook) {
    synchronized {
      val canAddHook = session.getBindingOf(h.tpe) match {
        case Some(b) if b.forSingleton =>
          !startHook.exists(_.tpe == h.tpe)
        case other =>
          true
      }
      if(canAddHook) {
        debug(s"Add start hook: ${h}")
        startHook :+= h
      }
    }
  }

  def addShutdownHook(h: LifeCycleHook) {
    synchronized {
      val canAddHook = session.getBindingOf(h.tpe) match {
        case Some(b) if b.forSingleton =>
          !shutdownHook.exists(_.tpe == h.tpe)
        case other =>
          true
      }
      if (canAddHook) {
        debug(s"Add shutdown hook: ${h}")
        shutdownHook :+= h
      }
    }
  }
}

object LifeCycleManager {
  def defaultLifeCycleEventHandler: LifeCycleEventHandler =
    ShowLifeCycleLog wraps defaultObjectLifeCycleHandler

  def defaultObjectLifeCycleHandler: LifeCycleEventHandler =
    LifeCycleAnnotationFinder andThen
      FILOLifeCycleHookExecutor andThen
    AddShutdownHook

}

object ShowLifeCycleLog extends LifeCycleEventHandler {
  private val logger = Logger.of[LifeCycleManager]

  override def beforeStart(lifeCycleManager:LifeCycleManager) {
    logger.info(s"[${lifeCycleManager.sessionName}] Life cycle is starting ...")
  }

  override def afterStart(lifeCycleManager:LifeCycleManager) {
    logger.info(s"[${lifeCycleManager.sessionName}] ======= STARTED =======")
  }

  override def beforeShutdown(lifeCycleManager:LifeCycleManager) {
    logger.info(s"[${lifeCycleManager.sessionName}] Stopping life cycle ...")
  }

  override def afterShutdown(lifeCycleManager:LifeCycleManager) {
    logger.info(s"[${lifeCycleManager.sessionName}] Life cycle has stopped.")
  }
}


object LifeCycleAnnotationFinder extends LifeCycleEventHandler with LogSupport {
  override def onInit(lifeCycleManager:LifeCycleManager, t: ObjectType, injectee: AnyRef) {
    val schema = ObjectSchema(injectee.getClass)

    // Find @PostConstruct annotation
    schema
    .allMethods
    .filter{_.findAnnotationOf[PostConstruct].isDefined}
    .map{x =>
      lifeCycleManager.addInitHook(ObjectMethodCall(t, injectee, x))
    }

    // Find @PreDestroy annotation
    schema
    .allMethods
    .filter {_.findAnnotationOf[PreDestroy].isDefined}
    .map { x =>
      lifeCycleManager.addShutdownHook(ObjectMethodCall(t, injectee, x))
    }
  }
}

/**
  * First In, Last Out (FILO) hook executor.
  *
  * If objects are injected in A -> B -> C order, init an shutdown orders will be:
  * init => A -> B -> C
  * shutdown order => C -> B -> A
  */
object FILOLifeCycleHookExecutor extends LifeCycleEventHandler with LogSupport {
  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    lifeCycleManager.startHooks.map { h =>
      trace(s"Calling start hook: $h")
      h.execute
    }
  }

  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    lifeCycleManager.shutdownHooks.reverse.map { h =>
      trace(s"Calling shutdown hook: $h")
      h.execute
    }
  }
}

object AddShutdownHook extends LifeCycleEventHandler {
  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    sys.addShutdownHook {
      lifeCycleManager.shutdown
    }
  }
}
