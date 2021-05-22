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

import wvlet.airframe.lifecycle.Injectee
import wvlet.airframe.surface.Surface
import wvlet.airframe.tracing.TraceEvent._
import wvlet.airframe.Session
import wvlet.log.LogSupport

/**
  * A base Tracer implementation for tracking DI events
  */
trait Tracer extends LogSupport {
  // Report tracing events
  protected def report(event: TraceEvent): Unit

  private[airframe] def onSessionInitStart(session: Session): Unit = {
    report(SessionInitStart(session))
  }
  private[airframe] def onSessionInitEnd(session: Session): Unit = {
    report(SessionInitEnd(session))
  }

  private[airframe] def onInjectStart(session: Session, surface: Surface): Unit = {
    report(InjectStart(session, surface))
  }

  private[airframe] def onInjectEnd(session: Session, surface: Surface): Unit = {
    report(InjectEnd(session, surface))
  }

  private[airframe] def onInitInstanceStart(session: Session, surface: Surface, injectee: Any): Unit = {
    report(InitInstanceStart(session, surface, injectee))
  }
  private[airframe] def onInitInstanceEnd(session: Session, surface: Surface, injectee: Any): Unit = {
    report(InitInstanceEnd(session, surface, injectee))
  }

  private[airframe] def onStartInstance(session: Session, injectee: Injectee): Unit = {
    report(StartInstance(session, injectee))
  }

  private[airframe] def afterStartInstance(session: Session, injectee: Injectee): Unit = {
    report(AfterStartInstance(session, injectee))
  }

  private[airframe] def beforeShutdownInstance(session: Session, injectee: Injectee): Unit = {
    report(BeforeShutdownInstance(session, injectee))
  }

  private[airframe] def onShutdownInstance(session: Session, injectee: Injectee): Unit = {
    report(ShutdownInstance(session, injectee))
  }

  private[airframe] def onSessionStart(session: Session): Unit = {
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
}

object DefaultTracer extends Tracer with LogSupport {
  override protected def report(event: TraceEvent): Unit = {
    trace(event)
  }
}
