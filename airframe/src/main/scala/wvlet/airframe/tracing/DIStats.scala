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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import wvlet.airframe.surface.Surface
import wvlet.airframe.{Design, Session}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters._

case class DIStatsReport(
    coverage: Double,
    observedTypes: Seq[Surface],
    initCount: Map[Surface, Long],
    injectCount: Map[Surface, Long],
    unusedTypes: Seq[Surface]
) {
  override def toString: String = {
    val report = Seq.newBuilder[String]

    // Coverage report
    report += "[coverage]"
    report += f"design coverage: ${coverage * 100}%.1f%%"
    if (unusedTypes.nonEmpty) {
      report += "[unused types]"
      unusedTypes.map { x => report += x.toString }
    }
    // Access stat report
    report += "[access stats]"
    val allTypes = injectCount.keySet ++ initCount.keySet
    for (s <- observedTypes) {
      report += s"[${s}] init:${initCount.getOrElse(s, 0)}, inject:${injectCount.getOrElse(s, 0)}"
    }

    report.result().mkString("\n")
  }
}

/**
  * DI statistics
  */
class DIStats extends LogSupport with Serializable {
  // This will holds the stat data while the session is active.
  // To avoid holding too many stats for applications that create many child sessions,
  // we will just store the aggregated stats.
  private val injectCountTable = new ConcurrentHashMap[Surface, AtomicLong]().asScala
  private val initCountTable   = new ConcurrentHashMap[Surface, AtomicLong]().asScala

  private val baseNano  = System.nanoTime()
  private val firstSeen = new ConcurrentHashMap[Surface, Long]().asScala

  private[airframe] def observe(s: Surface): Unit = {
    firstSeen.getOrElseUpdate(s, System.nanoTime() - baseNano)
  }

  private[airframe] def incrementInjectCount(session: Session, surface: Surface): Unit = {
    observe(surface)
    val counter = injectCountTable.getOrElseUpdate(surface, new AtomicLong(0))
    counter.incrementAndGet()
  }

  private[airframe] def incrementInitCount(session: Session, surface: Surface): Unit = {
    observe(surface)
    val counter = initCountTable.getOrElseUpdate(surface, new AtomicLong(0))
    counter.incrementAndGet()
  }

  private def getInjectCount(surface: Surface): Long = {
    injectCountTable.get(surface).map(_.get()).getOrElse(0)
  }

  def coverageReportFor(design: Design): DIStatsReport = {
    var bindingCount     = 0
    var usedBindingCount = 0
    val unusedBindings   = Seq.newBuilder[Surface]
    for (b <- design.binding) yield {
      bindingCount += 1
      val surface     = b.from
      val injectCount = getInjectCount(surface)
      if (injectCount > 0) {
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
    DIStatsReport(
      coverage = coverage,
      observedTypes = firstSeen.toSeq.sortBy(_._2).map(_._1).toSeq,
      initCount = initCountTable.map(x => x._1     -> x._2.get()).toMap,
      injectCount = injectCountTable.map(x => x._1 -> x._2.get()).toMap,
      unusedTypes = unusedBindings.result()
    )
  }
}
