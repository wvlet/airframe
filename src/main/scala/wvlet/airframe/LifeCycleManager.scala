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
    trace(s"Add init hook: ${h}")
    // Immediately execute the init hook
    h.execute
  }

  private var startHook    = List.empty[LifeCycleHook]
  private var shutdownHook = List.empty[LifeCycleHook]

  def startHooks: List[LifeCycleHook] = startHook
  def shutdownHooks: List[LifeCycleHook] = shutdownHook

  def addStartHook(h: LifeCycleHook) {
    trace(s"Add start hook: ${h}")
    synchronized {
      startHook = h :: startHook
    }
  }

  def addShutdownHook(h: LifeCycleHook) {
    trace(s"Add shutdown hook: ${h}")
    synchronized {
      shutdownHook = h :: shutdownHook
    }
  }
}

object LifeCycleManager {
  def defaultLifeCycleEventHandler: LifeCycleEventHandler =
    ShowLifeCycleLog wraps defaultObjectLifeCycleHandler

  def defaultObjectLifeCycleHandler: LifeCycleEventHandler =
    JSR330AnnotationHandler andThen
    FIFOHookExecutor andThen
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


object JSR330AnnotationHandler extends LifeCycleEventHandler with LogSupport {
  override def onInit(lifeCycleManager:LifeCycleManager, t: ObjectType, injectee: AnyRef) {
    debug(s"Injected ${t} (class: ${t.rawType}): $injectee")
    val schema = ObjectSchema(t.rawType)

    // Find JSR330 @PostConstruct annotation
    schema
    .allMethods
    .filter{_.findAnnotationOf[PostConstruct].isDefined}
    .map{x =>
      lifeCycleManager.addInitHook(ObjectMethodCall(injectee, x))
    }

    // Find JSR330 @PreDestroy annotation
    schema
    .allMethods
    .filter {_.findAnnotationOf[PreDestroy].isDefined}
    .map { x =>
      lifeCycleManager.addShutdownHook(ObjectMethodCall(injectee, x))
    }
  }
}

object FIFOHookExecutor extends LifeCycleEventHandler {
  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    lifeCycleManager.startHooks.reverse.map(_.execute)
  }

  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    lifeCycleManager.shutdownHooks.map(_.execute)
  }
}

object AddShutdownHook extends LifeCycleEventHandler {
  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    sys.addShutdownHook {
      lifeCycleManager.shutdown
    }
  }
}
