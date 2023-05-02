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
package wvlet.airframe.http

import wvlet.airframe.codec.MessageCodec
import wvlet.log.{LogLevel, LogSupport}

import scala.collection.immutable.ListMap

trait HttpLogWriter extends AutoCloseable {
  def write(log: Map[String, Any]): Unit
}

object HttpLogWriter extends LogSupport {

  /**
    * A log writer that writes logs to an in-memory buffer. This is useful for testing purpose.
    */
  def inMemoryLogWriter: InMemoryHttpLogWriter = new InMemoryHttpLogWriter()

  /**
    * In-memory log writer for testing purpose. Not for production use.
    */
  class InMemoryHttpLogWriter extends HttpLogWriter {
    private val logs = Seq.newBuilder[Map[String, Any]]

    def getLogs: Seq[Map[String, Any]] = logs.result()

    def clear(): Unit = {
      logs.clear()
    }

    override def write(log: Map[String, Any]): Unit = {
      synchronized {
        logs += log
      }
    }

    override def close(): Unit = {
      // no-op
    }
  }

  private val mapCodec = MessageCodec.of[Map[String, Any]]
  private def formatToJSON(log: Map[String, Any]): String = {
    mapCodec.toJson(log)
  }

  class ConsoleHttpLogWriter(logLevel: LogLevel) extends HttpLogWriter {
    override def write(log: Map[String, Any]): Unit = {
      logger.debug(formatToJSON(log))
    }
  }
}
