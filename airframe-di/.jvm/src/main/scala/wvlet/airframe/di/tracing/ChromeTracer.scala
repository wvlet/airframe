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
import java.io._

import wvlet.airframe.di.tracing.ChromeTracer._
import wvlet.airframe.di.tracing.TraceEvent._

/**
  * Tracer for producing DI events in Trace Event Format (JSON):
  * https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview
  *
  * This can be viewed with Google Chrome using chrome://tracing
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
    trace(event)

    // Using SessionID as process ID
    event match {
      case SessionInitStart(session) =>
        emit(
          Event(
            name = "process_name",
            cat = "metadata",
            ph = "M",
            ts = event.eventTimeMillis,
            pid = session.sessionId,
            tid = event.threadId,
            args = s"""{"name":"${session.name}"}"""
          )
        )
        emit(
          Event(
            name = s"${session.name}",
            cat = "session,init",
            ph = "B",
            ts = event.eventTimeMillis,
            pid = session.sessionId,
            tid = event.threadId
          )
        )
      case SessionInitEnd(session) =>
        emit(
          Event(
            name = s"${session.name}",
            cat = "session,init",
            ph = "E",
            ts = event.eventTimeMillis,
            pid = session.sessionId,
            tid = event.threadId
          )
        )
      case InjectStart(session, s) =>
        emit(
          Event(
            name = s"${s.name}",
            cat = "inject",
            ph = "B",
            ts = event.eventTimeMillis,
            pid = session.sessionId,
            tid = event.threadId
          )
        )
      case InjectEnd(session, s) =>
        emit(
          Event(
            name = s"${s.name}",
            cat = "inject",
            ph = "E",
            ts = event.eventTimeMillis,
            pid = session.sessionId,
            tid = event.threadId
          )
        )
      case InitInstanceStart(session, s, _) =>
        emit(
          Event(
            name = s"${s.name}",
            cat = "init",
            ph = "B",
            ts = event.eventTimeMillis,
            pid = session.sessionId,
            tid = event.threadId
          )
        )
      case InitInstanceEnd(session, s, _) =>
        emit(
          Event(
            name = s"${s.name}",
            cat = "init",
            ph = "E",
            ts = event.eventTimeMillis,
            pid = session.sessionId,
            tid = event.threadId
          )
        )
      case SessionStart(session) =>
        emit(
          Event(
            name = s"${session.name}",
            cat = "session",
            ph = "B",
            ts = event.eventTimeMillis,
            pid = session.sessionId,
            tid = event.threadId
          )
        )
      case SessionEnd(session) =>
        emit(
          Event(
            name = s"${session.name}",
            cat = "session",
            ph = "E",
            ts = event.eventTimeMillis,
            pid = session.sessionId,
            tid = event.threadId
          )
        )
        //out.println("]")
        out.flush()
      case _ =>
    }
  }
}

object ChromeTracer {

  /**
    * Create a chrome tracing format tracer to save the data to the given file
    *
    * @return
    */
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
    // Do not use airframe-json or codec here to avoid introducing extra dependencies.
    // airframe-di should have minimum dependencies (only airframe-log is included)
    def toJson: String =
      s"""{"name":"${name}","cat":"${cat}","ph":"${ph}","ts":${ts},"pid":${pid},"tid":${tid},"args":${args}}"""
  }
}
