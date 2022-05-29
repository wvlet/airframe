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
import java.util.concurrent.ConcurrentHashMap

import javax.annotation.PostConstruct
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

abstract class MetricLogger extends AutoCloseable {
  protected def tagPrefix: Option[String]
  def withTagPrefix(newTagPrefix: String): MetricLogger

  protected def emitRaw(fullTag: String, event: Map[String, Any]): Unit
  protected def emitRawMsgPack(fullTag: String, event: Array[Byte]): Unit

  def emit(tag: String, event: Map[String, Any]): Unit = {
    emitRaw(enrichTag(tag), event)
  }

  def emitMsgPack(tag: String, event: Array[Byte]): Unit = {
    emitRawMsgPack(enrichTag(tag), event)
  }

  private def enrichTag(tag: String): String = {
    tagPrefix match {
      case None => tag
      case Some(prefix) =>
        s"${prefix}.${tag}"
    }
  }

  @PostConstruct
  def start(): Unit = {}

  def flush(): Unit = {}
  override def close(): Unit
}

class TypedMetricLogger[T <: TaggedMetric](fluentdClient: MetricLogger, codec: MessageCodec[T]) {
  def emit(event: T): Unit = {
    fluentdClient.emitMsgPack(event.metricTag, codec.toMsgPack(event))
  }
}

class MetricLoggerFactory(
    fluentdClient: MetricLogger,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactory.withMapOutput
) extends MetricLoggerFactoryCompat
    with LogSupport
    with AutoCloseable {
  def getLogger: MetricLogger = fluentdClient
  def getLoggerWithTagPrefix(tagPrefix: String): MetricLogger =
    fluentdClient.withTagPrefix(tagPrefix)

  import scala.jdk.CollectionConverters._

  private val loggerCache = new ConcurrentHashMap[Surface, TypedMetricLogger[_]]().asScala

  protected def getTypedLoggerInternal[T <: TaggedMetric](
      surface: Surface,
      tagPrefix: Option[String]
  ): TypedMetricLogger[T] = {
    loggerCache
      .getOrElseUpdate(
        surface, {
          // Ensure to serialize as map type of MessagePack
          val codec = codecFactory.withMapOutput.of(surface).asInstanceOf[MessageCodec[T]]
          val metricLogger = tagPrefix match {
            case Some(prefix) => getLoggerWithTagPrefix(prefix)
            case _            => getLogger
          }
          new TypedMetricLogger[T](metricLogger, codec)
        }
      ).asInstanceOf[TypedMetricLogger[T]]
  }

  @PostConstruct
  def start: Unit = {
    debug("Starting MetricLoggerFactory")
  }

  def shutdown: Unit = {
    debug("Closing MetricLoggerFactory")
    fluentdClient.close()
  }

  override def close(): Unit = {
    shutdown
  }
}
