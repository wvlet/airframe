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
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

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

  // This will holds the stat data while the session is active.
  // To avoid holding too many stats for applications that create many child sessions,
  // we will just store the aggregated stats.
  private val injectCountTable = new ConcurrentHashMap[Surface, AtomicLong]().asScala
  private val bindCountTable   = new ConcurrentHashMap[Surface, AtomicLong]().asScala

  private[airframe] def incrementInjectCount(session: Session, surface: Surface): Unit = {
    val counter = injectCountTable.getOrElseUpdate(surface, new AtomicLong(0))
    counter.incrementAndGet()
  }

  private[airframe] def incrementGetBindingCount(session: Session, surface: Surface): Unit = {
    val counter = bindCountTable.getOrElseUpdate(surface, new AtomicLong(0))
    counter.incrementAndGet()
  }

  override def toString: String = {
    statsReport
  }

  def statsReport: String = {
    val b = Seq.newBuilder[String]
    for ((surface, counter) <- injectCountTable.toSeq.sortBy(_._2.get()).reverse) {
      b += s"[${surface}] injected:${counter.get}"
    }
    val report = b.result().mkString("\n")
    report
  }

  def getBindCount(surface: Surface): Long = {
    bindCountTable.get(surface).map(_.get()).getOrElse(0)
  }

  def coverageReport(d: Design): String = {
    var bindingCount     = 0
    var usedBindingCount = 0
    val unusedBindings   = Seq.newBuilder[Surface]
    for (b <- d.binding) yield {
      bindingCount += 1
      val surface   = b.from
      val bindCount = getBindCount(surface)
      if (bindCount > 0) {
        usedBindingCount += 1
      } else {
        unusedBindings += surface
      }
    }
    val coverage =
      if (bindingCount == 0) {
        1.0
      } else {
        usedBindingCount.toDouble / bindingCount
      }

    val report = Seq.newBuilder[String]

    report += f"design coverage: ${coverage * 100}%.1f%%"
    val unusedBindingList = unusedBindings.result()
    if (unusedBindingList.nonEmpty) {
      report += "[Unused Bindings]"
      unusedBindingList.map { x =>
        report += x.toString
      }
    }
    report.result().mkString("\n")
  }

}
