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

import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import org.msgpack.core.MessagePack
import scala.reflect.runtime.{universe => ru}
import wvlet.airframe._

/**
  * Object based metric logger. This automatically converts object into Map type values
  */
class MetricLogger[A](tag: String, codec: MessageCodec[A], fluentdClient: FluentdClient) {
  private val packer = MessagePack.newDefaultBufferPacker()

  def emit(metric: A): Unit = {
    // packer is non-thread safe
    synchronized {
      // Reuse the buffer
      packer.clear()
      // A -> MessagePack Map value
      codec.pack(packer, metric)
      val msgpack = packer.toByteArray
      fluentdClient.emitMsgPack(tag, msgpack)
    }
  }
}

/**
  * A factory for creating MetricLoggers
  */
trait MetricLoggerFactory {
  private val fluentdClient = bind[FluentdClient]
  // Use object -> Map value codec
  private val codecFactory = bind[MessageCodecFactory] { MessageCodec.defaultFactory.withObjectMapCodec }

  def newMetricLogger[A: ru.TypeTag](tag: String): MetricLogger[A] = {
    val codec = codecFactory.of[A]
    new MetricLogger(tag, codec, fluentdClient)
  }
}
