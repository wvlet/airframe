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

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.surface.Surface

abstract class MetricLogger(protected val tagPrefix: Option[String] = None) extends AutoCloseable {
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
}

class TypedMetricLogger[T](fluentdClient: MetricLogger, codec: MessageCodec[T]) {
  def emit(tag: String, event: T): Unit = {
    fluentdClient.emitMsgPack(tag, codec.toMsgPack(event))
  }
}

class MetricLoggerFactory(fluentdClient: MetricLogger) {
  def getLogger: MetricLogger = fluentdClient
  def getLoggerWithTagPrefix(tagPrefix: String): MetricLogger =
    fluentdClient.withTagPrefix(tagPrefix)

  import scala.collection.JavaConverters._
  import scala.reflect.runtime.{universe => ru}

  private val loggerCache = new ConcurrentHashMap[Surface, TypedMetricLogger[_]]().asScala

  def getTypedLogger[T: ru.TypeTag]: TypedMetricLogger[T] = {
    loggerCache
      .getOrElseUpdate(Surface.of[T], {
        val codec = MessageCodec.of[T]
        new TypedMetricLogger[T](getLogger, codec)
      }).asInstanceOf[TypedMetricLogger[T]]
  }

  def getTypedLoggerWithTagPrefix[T: ru.TypeTag](tagPrefix: String): TypedMetricLogger[T] = {
    loggerCache
      .getOrElseUpdate(Surface.of[T], {
        val codec = MessageCodec.of[T]
        new TypedMetricLogger[T](getLoggerWithTagPrefix(tagPrefix), codec)
      }).asInstanceOf[TypedMetricLogger[T]]
  }
}

class TDLoggerFactory(tdLogger: TDLogger)                extends MetricLoggerFactory(tdLogger)
class FluentdLoggerFactory(fluentdLogger: FluentdLogger) extends MetricLoggerFactory(fluentdLogger)
