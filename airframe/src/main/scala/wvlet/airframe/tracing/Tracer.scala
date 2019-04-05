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
package wvlet.airframe.tracing

import wvlet.airframe.surface.Surface
import wvlet.airframe.tracing.TraceEvent._
import wvlet.airframe.{Injectee, Session}
import wvlet.log.LogSupport

/**
  *
  */
trait Tracer extends LogSupport {

  private[airframe] def onSessionInitStart(session: Session): Unit = {
    report(SessionInitStart(session))
  }
  private[airframe] def onSessionInitEnd(session: Session): Unit = {
    report(SessionInitEnd(session))
  }

  private[airframe] def onGetBindingStart(session: Session, surface: Surface): Unit = {
    report(GetBindingStart(session, surface))
  }

  private[airframe] def onGetBindingEnd(session: Session, surface: Surface): Unit = {
    report(GetBindingEnd(session, surface))
  }

  private[airframe] def onInject(session: Session, surface: Surface, injectee: Any) = {

    report(InjectInstance(session, surface, injectee))
  }

  private[airframe] def onInitInstance(session: Session, injectee: Injectee): Unit = {
    report(InitInstance(session, injectee))
  }

  private[airframe] def onStartInstance(session: Session, injectee: Injectee): Unit = {
    report(StartInstance(session, injectee))
  }

  private[airframe] def beforeShutdownInstance(session: Session, injectee: Injectee): Unit = {
    report(BeforeShutdownInstance(session, injectee))
  }

  private[airframe] def onShutdownInstance(session: Session, injectee: Injectee): Unit = {
    report(ShutdownInstance(session, injectee))
  }

  private[airframe] def onSessionStart(session: Session) {
    report(SessionStart(session))
  }

  private[airframe] def beforeSessionShutdown(session: Session): Unit = {
    report(SessionBeforeShutdown(session))
  }

  private[airframe] def onSessionShutdown(session: Session): Unit = {
    report(SessionShutdown(session))
  }
  private[airframe] def onSessionEnd(session: Session): Unit = {
    report(SessionEnd(session))
  }

  protected def report(event: TraceEvent): Unit
}

sealed trait TraceEvent {
  val eventTimeMillis = System.currentTimeMillis()
  val threadId        = Thread.currentThread().getId
}

object TraceEvent {
  case class SessionInitStart(session: Session)      extends TraceEvent
  case class SessionInitEnd(session: Session)        extends TraceEvent
  case class SessionStart(session: Session)          extends TraceEvent
  case class SessionBeforeShutdown(session: Session) extends TraceEvent
  case class SessionShutdown(session: Session)       extends TraceEvent
  case class SessionEnd(session: Session)            extends TraceEvent

  case class GetBindingStart(session: Session, s: Surface)                extends TraceEvent
  case class GetBindingEnd(session: Session, s: Surface)                  extends TraceEvent
  case class InjectInstance(session: Session, s: Surface, any: Any)       extends TraceEvent
  case class InitInstance(session: Session, injectee: Injectee)           extends TraceEvent
  case class StartInstance(session: Session, injectee: Injectee)          extends TraceEvent
  case class BeforeShutdownInstance(session: Session, injectee: Injectee) extends TraceEvent
  case class ShutdownInstance(session: Session, injectee: Injectee)       extends TraceEvent
}

class DefaultTracer extends Tracer with LogSupport {

  override protected def report(event: TraceEvent): Unit = {
    trace(event)
  }
}
