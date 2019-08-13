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
import org.msgpack.core.MessagePack
import wvlet.log.{LogLevel, LogSupport}

/**
  * Fluentd client implementation for debugging. This just emits metrics to the console log
  */
class ConsoleLogger(val tagPrefix: Option[String] = None, logLevel: LogLevel = LogLevel.INFO)
    extends MetricLogger
    with LogSupport {

  override def withTagPrefix(newTagPrefix: String): ConsoleLogger = {
    new ConsoleLogger(Some(newTagPrefix))
  }

  override protected def emitRaw(tag: String, event: Map[String, Any]): Unit = {
    logAt(logLevel, s"${tag}: ${event.mkString(", ")}")
  }
  override protected def emitRawMsgPack(tag: String, event: Array[Byte]): Unit = {
    val unpacker = MessagePack.newDefaultUnpacker(event)
    val v        = unpacker.unpackValue()
    unpacker.close()
    logAt(logLevel, s"${tag}: ${v}")
  }
  override def close(): Unit = {}
}
