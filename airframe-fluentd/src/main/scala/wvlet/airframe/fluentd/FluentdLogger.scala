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
package wvlet.airframe.fluentd
import java.time.Instant

import javax.annotation.PreDestroy
import org.komamitsu.fluency.{EventTime, Fluency}
import wvlet.log.LogSupport

class FluentdLogger(val tagPrefix: Option[String] = None, useExtendedEventTime: Boolean, fluency: Fluency)
    extends MetricLogger
    with LogSupport {

  info(s"Starting Fluency")

  override def withTagPrefix(newTagPrefix: String): FluentdLogger = {
    new FluentdLogger(Some(newTagPrefix), useExtendedEventTime, fluency)
  }

  @PreDestroy
  def close: Unit = {
    info(s"Stopping Fluency")
    fluency.flush()
    fluency.close()
  }

  private def getEventTime: EventTime = {
    val now       = Instant.now()
    val eventTime = EventTime.fromEpoch(now.getEpochSecond.toInt, now.getNano.toInt)
    eventTime
  }

  override def emitRaw(fullTag: String, event: Map[String, Any]): Unit = {
    if (useExtendedEventTime) {
      fluency.emit(fullTag, getEventTime, toJavaMap(event))
    } else {
      fluency.emit(fullTag, toJavaMap(event))
    }
  }
  override def emitRawMsgPack(tag: String, event: Array[Byte]): Unit = {
    if (useExtendedEventTime) {
      fluency.emit(tag, getEventTime, event, 0, event.length)
    } else {
      fluency.emit(tag, event, 0, event.length)
    }
  }

  private def toJavaMap(event: Map[String, Any]): java.util.Map[String, AnyRef] = {
    import scala.collection.JavaConverters._
    (for ((k, v) <- event) yield {
      k -> v.asInstanceOf[AnyRef]
    }).asJava
  }

}
