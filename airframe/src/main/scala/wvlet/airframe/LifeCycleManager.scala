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

import wvlet.log.{LogSupport, Logger}
import wvlet.airframe.surface.Surface

sealed trait LifeCycleStage
case object INIT     extends LifeCycleStage
case object STARTING extends LifeCycleStage
case object STARTED  extends LifeCycleStage
case object STOPPING extends LifeCycleStage
case object STOPPED  extends LifeCycleStage

/**
  * LifeCycleManager manages the life cycle of objects within a Session
  */
class LifeCycleManager(eventHandler: LifeCycleEventHandler) extends LogSupport {
  self =>

  private val state                = new AtomicReference[LifeCycleStage](INIT)
  def currentState: LifeCycleStage = state.get()

  private[airframe] def onInit(t: Surface, injectee: AnyRef): Unit = {
    eventHandler.onInit(this, t, injectee)
  }

  private var session: Session = _
  private[airframe] def setSession(s: Session): Unit = {
    session = s
  }

  def sessionName: String = session.name

  def start: Unit = {
    if (!state.compareAndSet(INIT, STARTING)) {
      throw new IllegalStateException(s"LifeCycle is already starting")
    }

    eventHandler.beforeStart(this)
    // Run start hooks in the registration order
    state.set(STARTED)
    eventHandler.afterStart(this)
  }

  def shutdown: Unit = {
    if (state.compareAndSet(STARTED, STOPPING) || state.compareAndSet(INIT, STOPPING)
        || state.compareAndSet(STARTING, STOPPING)) {
      eventHandler.beforeShutdown(this)
      // Run shutdown hooks in the reverse registration order
      state.set(STOPPED)
      eventHandler.afterShutdown(this)
    }
  }

  private var initializedSingleton = Set.empty[Surface]
  private var startHook            = Vector.empty[LifeCycleHook]
  private var preShutdownHook      = Vector.empty[LifeCycleHook]
  private var shutdownHook         = Vector.empty[LifeCycleHook]

  def startHooks: Seq[LifeCycleHook]       = startHook
  def preShutdownHooks: Seq[LifeCycleHook] = preShutdownHook
  def shutdownHooks: Seq[LifeCycleHook]    = shutdownHook

  private def isSingletonType(t: Surface): Boolean = {
    session.getBindingOf(t).exists(_.forSingleton) || session.hasSingletonOf(t)
  }

  def addInitHook(h: LifeCycleHook): Unit = {
    debug(s"Add init hook: ${h.surface}")
    val canRunHook = !(isSingletonType(h.surface) && initializedSingleton.contains(h.surface))
    if (canRunHook) {
      initializedSingleton += h.surface
      h.execute
    }
  }

  def addInjectHook(h: LifeCycleHook): Unit = {
    debug(s"Add inject hook: ${h.surface}")
    // Run immediately
    h.execute
  }

  def addStartHook(h: LifeCycleHook): Unit = {
    synchronized {
      val canAddHook = !(isSingletonType(h.surface) && startHook.exists(_.surface == h.surface))
      if (canAddHook) {
        debug(s"Add start hook for ${h.surface}")
        val s = state.get
        startHook :+= h
        if (s == STARTED) {
          // If a session is already started, run the start hook immediately
          h.execute
        }
      }
    }
  }

  def addPreShutdownHook(h: LifeCycleHook): Unit = {
    synchronized {
      val canAddHook = !(isSingletonType(h.surface) && preShutdownHook.exists(_.surface == h.surface))
      if (canAddHook) {
        debug(s"Add pre-shutdown hook for ${h.surface}")
        preShutdownHook :+= h
      }
    }
  }

  def addShutdownHook(h: LifeCycleHook): Unit = {
    synchronized {
      val canAddHook = !(isSingletonType(h.surface) && shutdownHook.exists(_.surface == h.surface))
      if (canAddHook) {
        debug(s"Add shutdown hook for ${h.surface}")
        shutdownHook :+= h
      }
    }
  }
}

object LifeCycleManager {
  def defaultLifeCycleEventHandler: LifeCycleEventHandler =
    ShowLifeCycleLog wraps mandatoryObjectLifeCycleHandler

  def mandatoryObjectLifeCycleHandler: LifeCycleEventHandler =
    FILOLifeCycleHookExecutor andThen JSR250LifeCycleExecutor andThen AddShutdownHook
}

object ShowLifeCycleLog extends LifeCycleEventHandler {
  private val logger = Logger.of[LifeCycleManager]

  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    logger.info(s"[${lifeCycleManager.sessionName}] Starting a new life cycle ...")
  }

  override def afterStart(lifeCycleManager: LifeCycleManager): Unit = {
    logger.info(s"[${lifeCycleManager.sessionName}] ======== STARTED ========")
  }

  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    logger.info(s"[${lifeCycleManager.sessionName}] Stopping the life cycle ...")
  }

  override def afterShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    logger.info(s"[${lifeCycleManager.sessionName}] The life cycle has stopped.")
  }
}

object ShowDebugLifeCycleLog extends LifeCycleEventHandler {
  private val logger = Logger.of[LifeCycleManager]

  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    logger.debug(s"[${lifeCycleManager.sessionName}] Starting a new life cycle ...")
  }

  override def afterStart(lifeCycleManager: LifeCycleManager): Unit = {
    logger.debug(s"[${lifeCycleManager.sessionName}] ======== STARTED ========")
  }

  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    logger.debug(s"[${lifeCycleManager.sessionName}] Stopping the life cycle ...")
  }

  override def afterShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    logger.debug(s"[${lifeCycleManager.sessionName}] The life cycle has stopped.")
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
    // beforeShutdown
    for (h <- lifeCycleManager.preShutdownHooks.reverse) {
      trace(s"Calling pre-shutdown hook: $h")
      h.execute
    }

    // onShutdown
    val shutdownOrder = lifeCycleManager.shutdownHooks.reverse
    debug(s"Shutdown order:\n${shutdownOrder.mkString("\n-> ")}")
    shutdownOrder.map { h =>
      trace(s"Calling shutdown hook: $h")
      h.execute
    }
  }
}
