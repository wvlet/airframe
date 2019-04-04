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
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import TraceEvent._

/**
  *
  */
trait Tracer extends LogSupport {
  def report(event: TraceEvent): Unit

  def onSessionInitStart(session: Session): Unit = {
    report(SessionInitStart(session))
  }
  def onSessionInitEnd(session: Session): Unit = {
    report(SessionInitEnd(session))
  }

  def onGetBinding(surface: Surface): Unit = {
    report(GetBinding(surface))
  }

  def onInject(surface: Surface, injectee: Any) = {
    report(Inject(surface, injectee))
  }

  def onInitInstance(injectee: Injectee): Unit = {
    report(InitInstance(injectee))
  }

  def onStartInstance(injectee: Injectee): Unit = {
    report(StartInstance(injectee))
  }

  def beforeShutdownInstance(injectee: Injectee): Unit = {
    report(BeforeShutdownInstance(injectee))
  }

  def onShutdownInstance(injectee: Injectee): Unit = {
    report(ShutdownInstance(injectee))
  }

  def onSessionStart(session: Session) {
    report(SessionStart(session))
  }

  def beforeSessionShutdown(session: Session): Unit = {
    report(SessionBeforeShutdown(session))
  }

  def onSessionShutdown(session: Session): Unit = {
    report(SessionShutdown(session))
  }
  def onSessionEnd(session: Session): Unit = {
    report(SessionEnd(session))
  }
}

sealed trait TraceEvent

object TraceEvent {
  case class SessionInitStart(session: Session)      extends TraceEvent
  case class SessionInitEnd(session: Session)        extends TraceEvent
  case class SessionStart(session: Session)          extends TraceEvent
  case class SessionBeforeShutdown(session: Session) extends TraceEvent
  case class SessionShutdown(session: Session)       extends TraceEvent
  case class SessionEnd(session: Session)            extends TraceEvent

  case class GetBinding(s: Surface)                     extends TraceEvent
  case class Inject(s: Surface, any: Any)               extends TraceEvent
  case class InitInstance(injectee: Injectee)           extends TraceEvent
  case class StartInstance(injectee: Injectee)          extends TraceEvent
  case class BeforeShutdownInstance(injectee: Injectee) extends TraceEvent
  case class ShutdownInstance(injectee: Injectee)       extends TraceEvent
}

object DefaultTracer extends Tracer with LogSupport {

  override def report(event: TraceEvent): Unit = {
    warn(event)
  }

}
