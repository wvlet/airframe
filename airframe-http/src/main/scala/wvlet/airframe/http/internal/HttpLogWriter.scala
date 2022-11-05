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
package wvlet.airframe.http.internal

import scala.collection.immutable.Map

trait HttpLogWriter extends AutoCloseable {
  def write(log: Map[String, Any]): Unit
}

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
