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
import java.io._

import wvlet.airframe.Session
import wvlet.airframe.tracing.TraceEvent._
import ChromeTracer._

/**
  * Trace that produces DI events in Trace Event Format of Google Chrome:
  * https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview
  */
class ChromeTracer(s: OutputStream) extends Tracer {
  private val out = new PrintWriter(s)

  // Output JSON array start
  out.println("[")

  protected def emit(ev: Event): Unit = {
    out.print(ev.toJson)
    out.println(",")
  }

  override protected def report(event: TraceEvent): Unit = {
    warn(event)
    event match {
      case SessionInitStart(session) =>
        emit(
          Event(
            name = s"${session.name}",
            cat = "session",
            ph = "B",
            ts = event.eventTimeMillis,
            pid = 0, // TODO
            tid = event.threadId // TODO
          ))
        emit(
          Event(
            name = s"${session.name} init",
            cat = "session",
            ph = "B",
            ts = event.eventTimeMillis,
            pid = 0, // TODO
            tid = event.threadId // TODO
          ))
      case SessionInitEnd(session) =>
        emit(
          Event(
            name = s"${session.name} init",
            cat = "session",
            ph = "E",
            ts = event.eventTimeMillis,
            pid = 0, // TODO
            tid = event.threadId // TODO
          ))
      case GetBindingStart(session, s) =>
        emit(
          Event(
            name = s"${s.name}",
            cat = "inject",
            ph = "B",
            ts = event.eventTimeMillis,
            pid = 0, // TODO
            tid = event.threadId // TODO
          ))
      case GetBindingEnd(session, s) =>
        emit(
          Event(
            name = s"${s.name}",
            cat = "inject",
            ph = "E",
            ts = event.eventTimeMillis,
            pid = 0, // TODO
            tid = event.threadId // TODO
          ))
      case SessionStart(session) =>
//        emit(dd
//          Event(
//            name = s"${session.name}",
//            cat = "session",
//            ph = "B",
//            ts = event.eventTimeMillis,
//            pid = 0, // TODO
//            tid = event.threadId // TODO
//          ))
      case SessionEnd(session) =>
        emit(
          Event(
            name = s"${session.name}",
            cat = "session",
            ph = "E",
            ts = event.eventTimeMillis,
            pid = 0, // TODO
            tid = event.threadId // TODO
          ))
        //out.println("]")
        out.flush()
      case _ =>
    }

  }
  override protected def reportStats(session: Session, stats: AirframeStats): Unit = {}
}

object ChromeTracer {
  def newTracer(fileName: String): ChromeTracer = {
    new ChromeTracer(new BufferedOutputStream(new FileOutputStream(fileName)))
  }

  /**
    * name: The name of the event, as displayed in Trace Viewer
    * cat: The event categories. This is a comma separated list of categories for the event. The categories can be used to hide events in the Trace Viewer UI.
    * ph: The event type. This is a single character which changes depending on the type of event being output. The valid values are listed in the table below. We will discuss each phase type below.
    * ts: The tracing clock timestamp of the event. The timestamps are provided at microsecond granularity.
    * tts: Optional. The thread clock timestamp of the event. The timestamps are provided at microsecond granularity.
    * pid: The process ID for the process that output this event.
    * tid: The thread ID for the thread that output this event.
    * args: Any arguments provided for the event. Some of the event types have required argument fields, otherwise, you can put any information you wish in here. The arguments are displayed in Trace Viewer when you view an event in the analysis section.
    */
  case class Event(name: String, cat: String, ph: String, ts: Long, pid: Long, tid: Long, args: String = "{}") {
    def toJson: String =
      s"""{"name":"${name}","cat":"${cat}","ph":"${ph}","ts":${ts},"pid":${pid},"tid":${tid},"args":${args}}"""
  }

}
