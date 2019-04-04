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

import Tracer._

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

  def onGetInstance(surface: Surface): Unit = {
    report(GetInstance(surface))
  }
}

trait TraceEvent

object Tracer {
  case class SessionInitStart(session: Session) extends TraceEvent
  case class SessionInitEnd(session: Session)   extends TraceEvent
  case class SessionStart(session: Session)     extends TraceEvent

  case class GetInstance(s: Surface) extends TraceEvent
}

object DefaultTracer extends Tracer with LogSupport {

  override def report(event: TraceEvent): Unit = {
    warn(event)
  }

}
