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
package wvlet.airframe.lifecycle

import java.util.concurrent.atomic.AtomicReference

import wvlet.airframe.AirframeException.{MULTIPLE_SHUTDOWN_FAILURES, SHUTDOWN_FAILURE}
import wvlet.airframe.surface.Surface
import wvlet.airframe.tracing.Tracer
import wvlet.airframe.{AirframeSession, Session}
import wvlet.log.{LogSupport, Logger}

import scala.util.control.NonFatal

sealed trait LifeCycleStage
case object INIT     extends LifeCycleStage
case object STARTING extends LifeCycleStage
case object STARTED  extends LifeCycleStage
case object STOPPING extends LifeCycleStage
case object STOPPED  extends LifeCycleStage

sealed trait LifeCycleHookType
case object ON_INIT         extends LifeCycleHookType
case object ON_INJECT       extends LifeCycleHookType
case object ON_START        extends LifeCycleHookType
case object AFTER_START     extends LifeCycleHookType
case object BEFORE_SHUTDOWN extends LifeCycleHookType
case object ON_SHUTDOWN     extends LifeCycleHookType

/**
  * LifeCycleManager manages the life cycle of objects within a Session
  */
class LifeCycleManager(
    private[airframe] val eventHandler: LifeCycleEventHandler,
    // EventHandler without lifecycle logger and shutdown hook
    private[airframe] val coreEventHandler: LifeCycleEventHandler
) extends LogSupport {
  self =>

  import LifeCycleManager._

  private val state                = new AtomicReference[LifeCycleStage](INIT)
  def currentState: LifeCycleStage = state.get()

  private[airframe] def onInit(t: Surface, injectee: AnyRef): Unit = {
    eventHandler.onInit(this, t, injectee)
  }

  // Session and tracer will be available later
  private[airframe] var session: AirframeSession = _
  private[airframe] var tracer: Tracer           = _

  private[airframe] def setSession(s: AirframeSession): Unit = {
    session = s
    tracer = session.tracer
  }

  def sessionName: String = session.name

  def start: Unit = {
    if (!state.compareAndSet(INIT, STARTING)) {
      throw new IllegalStateException("LifeCycle is already starting")
    }

    tracer.onSessionStart(session)
    eventHandler.beforeStart(this)
    // Run start hooks in the registration order
    state.set(STARTED)
    eventHandler.afterStart(this)
  }

  def shutdown: Unit = {
    if (state.compareAndSet(STARTED, STOPPING) || state.compareAndSet(INIT, STOPPING)
        || state.compareAndSet(STARTING, STOPPING)) {
      tracer.beforeSessionShutdown(session)
      eventHandler.beforeShutdown(this)
      // Run shutdown hooks in the reverse registration order
      state.set(STOPPED)

      tracer.onSessionShutdown(session)
      eventHandler.afterShutdown(this)
      tracer.onSessionEnd(session)
    }
  }

  private[airframe] var initHookHolder        = new LifeCycleHookHolder
  private[airframe] var startHookHolder       = new LifeCycleHookHolder
  private[airframe] var afterStartHookHolder  = new LifeCycleHookHolder
  private[airframe] var preShutdownHookHolder = new LifeCycleHookHolder
  private[airframe] var shutdownHookHolder    = new LifeCycleHookHolder

  def startHooks: Seq[LifeCycleHook]       = startHookHolder.list
  def afterStartHooks: Seq[LifeCycleHook]  = afterStartHookHolder.list
  def preShutdownHooks: Seq[LifeCycleHook] = preShutdownHookHolder.list
  def shutdownHooks: Seq[LifeCycleHook]    = shutdownHookHolder.list

  protected def findLifeCycleManagerFor[U](s: Surface)(body: LifeCycleManager => U): U = {
    // Adding a lifecycle hook in the owner session
    session.findOwnerSessionOf(s) match {
      case Some(s) => body(s.lifeCycleManager)
      case None    => body(this)
    }
  }

  private[airframe] def hasHooksFor(s: Surface, lifeCycleHookType: LifeCycleHookType): Boolean = {
    findLifeCycleManagerFor(s) { l =>
      lifeCycleHookType match {
        case ON_INIT         => l.initHookHolder.hasHooksFor(s)
        case ON_INJECT       => false
        case ON_START        => l.startHookHolder.hasHooksFor(s)
        case AFTER_START     => l.afterStartHookHolder.hasHooksFor(s)
        case BEFORE_SHUTDOWN => l.preShutdownHookHolder.hasHooksFor(s)
        case ON_SHUTDOWN     => l.shutdownHookHolder.hasHooksFor(s)
      }
    }
  }

  private[airframe] def hasShutdownHooksFor(s: Surface): Boolean = {
    hasHooksFor(s, ON_SHUTDOWN)
  }

  private[airframe] def addLifeCycleHook(lifeCycleHookType: LifeCycleHookType, h: LifeCycleHook): Unit = {
    trace(s"Adding a life cycle hook for ${lifeCycleHookType}: ${h.surface}")
    lifeCycleHookType match {
      case ON_INIT =>
        addInitHook(h)
      case ON_INJECT =>
        addInjectHook(h)
      case ON_START =>
        addStartHook(h)
      case AFTER_START =>
        addAfterStartHook(h)
      case BEFORE_SHUTDOWN =>
        addPreShutdownHook(h)
      case ON_SHUTDOWN =>
        addShutdownHook(h)
    }
  }

  def addInitHook(h: LifeCycleHook): Unit = {
    findLifeCycleManagerFor(h.surface) { l =>
      if (l.initHookHolder.registerOnlyOnce(h)) {
        debug(s"[${l.sessionName}] Add an init hook: ${h.surface}")
        h.execute
      } else {
        trace(s"[${l.sessionName}] ${h.injectee} is already initialized")
      }
    }
  }

  def addInjectHook(h: LifeCycleHook): Unit = {
    findLifeCycleManagerFor(h.surface) { l =>
      debug(s"[${l.sessionName}] Running an inject hook: ${h.surface}")
      // Run immediately
      h.execute
    }
  }

  def addStartHook(h: LifeCycleHook): Unit = {
    findLifeCycleManagerFor(h.surface) { l =>
      l.synchronized {
        if (l.startHookHolder.registerOnlyOnce(h)) {
          debug(s"[${l.sessionName}] Add a start hook for ${h.surface}")
          val s = l.state.get
          if (s == STARTED) {
            // If a session is already started, run the start hook immediately
            tracer.onStartInstance(session, h.injectee)
            h.execute
          }
        }
      }
    }
  }

  def addAfterStartHook(h: LifeCycleHook): Unit = {
    findLifeCycleManagerFor(h.surface) { l =>
      l.synchronized {
        if (l.afterStartHookHolder.registerOnlyOnce(h)) {
          debug(s"[${l.sessionName}] Add a afterStart hook for ${h.surface}")
          val s = l.state.get
          if (s == STARTED) {
            // If a session is already started, run the start hook immediately
            tracer.afterStartInstance(session, h.injectee)
            h.execute
          }
        }
      }
    }
  }

  def addPreShutdownHook(h: LifeCycleHook): Unit = {
    findLifeCycleManagerFor(h.surface) { l =>
      l.synchronized {
        if (l.preShutdownHookHolder.registerOnlyOnce(h)) {
          debug(s"[${l.sessionName}] Add a pre-shutdown hook for ${h.surface}")
        }
      }
    }
  }

  def addShutdownHook(h: LifeCycleHook): Unit = {
    findLifeCycleManagerFor(h.surface) { l =>
      l.synchronized {
        if (l.shutdownHookHolder.registerOnlyOnce(h)) {
          debug(s"[${l.sessionName}] Add a shutdown hook for ${h.surface}")
        } else {
          // Override CloseHooks
          val previousHooks = l.shutdownHookHolder.hooksFor(h.injectee)
          previousHooks
            .collect { case c: CloseHook => c }
            .foreach { c =>
              // Any custom shutdown hooks precede CloseHook
              l.shutdownHookHolder.remove(c)
            }
          if (l.shutdownHookHolder.registerOnlyOnce(h)) {
            debug(s"[${l.sessionName}] Override CloseHook of ${h.surface} with a shtudown hook")
          }
        }
      }
    }
  }
}

object LifeCycleManager {
  private[airframe] class LifeCycleHookHolder(private var holder: Vector[LifeCycleHook] = Vector.empty) {
    def list: Seq[LifeCycleHook] = holder

    def hasHooksFor(s: Surface): Boolean = {
      synchronized {
        list.exists(_.surface == s)
      }
    }

    def remove(x: LifeCycleHook): Unit = {
      synchronized {
        holder = holder.filter(_ ne x)
      }
    }

    def hooksFor(x: Injectee): Seq[LifeCycleHook] = {
      synchronized {
        list.filter(_.injectee == x)
      }
    }

    /**
      *  Return true if it is not yet registered
      */
    def registerOnlyOnce(x: LifeCycleHook): Boolean = {
      synchronized {
        if (list.exists(_.injectee == x.injectee)) {
          false
        } else {
          // Register this hook
          holder :+= x
          true
        }
      }
    }
  }

  def defaultLifeCycleEventHandler: LifeCycleEventHandler =
    FILOLifeCycleHookExecutor andThen
      JSR250LifeCycleExecutor
}

object ShowLifeCycleLog extends LifeCycleEventHandler {
  private val logger = Logger.of[LifeCycleManager]

  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    logger.info(s"[${lifeCycleManager.sessionName}] Starting a new lifecycle ...")
  }

  override def afterStart(lifeCycleManager: LifeCycleManager): Unit = {
    logger.info(s"[${lifeCycleManager.sessionName}] ======== STARTED ========")
  }

  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    logger.info(s"[${lifeCycleManager.sessionName}] Stopping the lifecycle ...")
  }

  override def afterShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    logger.info(s"[${lifeCycleManager.sessionName}] The lifecycle has stopped.")
  }
}

object ShowDebugLifeCycleLog extends LifeCycleEventHandler {
  private val logger = Logger.of[LifeCycleManager]

  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    logger.debug(s"[${lifeCycleManager.sessionName}] Starting a new lifecycle ...")
  }

  override def afterStart(lifeCycleManager: LifeCycleManager): Unit = {
    logger.debug(s"[${lifeCycleManager.sessionName}] ======== STARTED ========")
  }

  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    logger.debug(s"[${lifeCycleManager.sessionName}] Stopping the lifecycle ...")
  }

  override def afterShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    logger.debug(s"[${lifeCycleManager.sessionName}] The lifecycle has stopped.")
  }
}

/**
  * First In, Last Out (FILO) hook executor.
  *
  * If objects are injected in A -> B -> C order, the init and shutdown orders will be as follows:
  * init hook call order: A -> B -> C
  * shutdown hook call order: C -> B -> A
  */
object FILOLifeCycleHookExecutor extends LifeCycleEventHandler with LogSupport {
  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    lifeCycleManager.startHooks.map { h =>
      trace(s"Calling start hook: $h")
      h.execute
    }
  }

  override def afterStart(lifeCycleManager: LifeCycleManager): Unit = {
    lifeCycleManager.afterStartHooks.map { h =>
      trace(s"Calling afterStart hook: $h")
      h.execute
    }
  }

  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    var exceptionList = List.empty[Throwable]

    // beforeShutdown
    for (h <- lifeCycleManager.preShutdownHooks.reverse) {
      trace(s"Calling pre-shutdown hook: $h")
      lifeCycleManager.tracer.beforeShutdownInstance(lifeCycleManager.session, h.injectee)
      try {
        h.execute
      } catch {
        case NonFatal(e) =>
          exceptionList = e :: exceptionList
      }
    }

    // onShutdown
    val shutdownOrder = lifeCycleManager.shutdownHooks.reverse
    if (shutdownOrder.nonEmpty) {
      debug(s"[${lifeCycleManager.sessionName}] Shutdown order:\n${shutdownOrder.map(x => s"-> ${x}").mkString("\n")}")
    }
    shutdownOrder.map { h =>
      trace(s"Calling shutdown hook: $h")
      lifeCycleManager.tracer.onShutdownInstance(lifeCycleManager.session, h.injectee)
      try {
        h.execute
      } catch {
        case NonFatal(e) =>
          exceptionList = e :: exceptionList
      }
    }

    // If there are any exceptions occurred during the shutdown, throw them here:
    if (exceptionList.nonEmpty) {
      if (exceptionList.size > 1) {
        throw MULTIPLE_SHUTDOWN_FAILURES(exceptionList.toList)
      } else {
        throw SHUTDOWN_FAILURE(exceptionList.head)
      }
    }
  }
}

class CloseHook(val injectee: Injectee) extends LifeCycleHook {
  override def toString: String = s"CloseHook for [${surface}]"
  override def execute: Unit = {
    injectee.injectee match {
      case c: AutoCloseable =>
        c.close()
      case _ =>
    }
  }
}
