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
package wvlet.airframe.di.tracing
import wvlet.airframe.di.lifecycle.Injectee
import wvlet.airframe.surface.Surface
import wvlet.airframe.di.Session

/**
  * Tracing event
  */
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

  case class InjectStart(session: Session, s: Surface)                      extends TraceEvent
  case class InjectEnd(session: Session, s: Surface)                        extends TraceEvent
  case class InitInstanceStart(session: Session, s: Surface, injectee: Any) extends TraceEvent
  case class InitInstanceEnd(session: Session, s: Surface, injectee: Any)   extends TraceEvent
  case class StartInstance(session: Session, injectee: Injectee)            extends TraceEvent
  case class AfterStartInstance(session: Session, injectee: Injectee)       extends TraceEvent
  case class BeforeShutdownInstance(session: Session, injectee: Injectee)   extends TraceEvent
  case class ShutdownInstance(session: Session, injectee: Injectee)         extends TraceEvent
}
