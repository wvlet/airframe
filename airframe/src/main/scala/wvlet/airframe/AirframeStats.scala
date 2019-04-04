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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.collection.JavaConverters._

// A table to lookup V from a pair of Row and Col
class LookupTable[Row, Col, V] extends Iterable[(Row, Map[Col, V])] {
  private val table = new ConcurrentHashMap[Row, scala.collection.concurrent.Map[Col, V]].asScala

  def getOrElseUpdate(row: Row, col: Col, defaultValue: V): V = {
    val m = table.getOrElseUpdate(row, new ConcurrentHashMap[Col, V]().asScala)
    m.getOrElseUpdate(col, defaultValue)
  }

  def rowKeys: Iterable[Row] = {
    table.keys
  }

  def row(rowKey: Row): Map[Col, V] = {
    table.get(rowKey).map(_.toMap).getOrElse(Map.empty)
  }

  override def iterator: Iterator[(Row, Map[Col, V])] = table.map(x => (x._1, x._2.toMap)).iterator
}

/**
  *
  */
class AirframeStats extends LogSupport {

  // TODO: This tracer will keep holding stats for a long time,
  // so if an application creates a lot of child sessions, storing stats for each session
  // will consume a lot of memory

  // Use session id as a key so as not to hold the Session reference
  private val injectCountTable = new LookupTable[String, Surface, AtomicInteger]()

  private[airframe] def incrementInjectCount(session: Session, surface: Surface): Unit = {
    val counter = injectCountTable.getOrElseUpdate(session.name, surface, new AtomicInteger(0))
    counter.incrementAndGet()
  }

  override def toString: String = {
    statsReport
  }

  def statsReport: String = {
    val b = Seq.newBuilder[String]
    for (sessionName <- injectCountTable.rowKeys) {
      b += s"[${sessionName}]"
      for ((surface, counter) <- injectCountTable.row(sessionName).toSeq.sortBy(_._2.get()).reverse) {
        b += s"[${surface}] injected:${counter.get}"
      }
    }
    val report = b.result().mkString("\n")
    report
  }

}
