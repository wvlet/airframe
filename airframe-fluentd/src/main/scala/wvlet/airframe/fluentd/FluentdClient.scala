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
import wvlet.airframe._

case class FluentdTag(
    // A tag prefix prepended to each message
    prefix: String = ""
)

trait FluentdClient {
  private val fluentdTag = bind[FluentdTag]

  protected def emitRaw(fullTag: String, event: Map[String, Any]): Unit
  protected def emitRawMsgPack(fullTag: String, event: Array[Byte]): Unit

  def emit(tag: String, event: Map[String, Any]): Unit = {
    emitRaw(enrichTag(tag), event)
  }

  def emitMsgPack(tag: String, event: Array[Byte]): Unit = {
    emitRawMsgPack(enrichTag(tag), event)
  }

  private def enrichTag(tag: String): String = {
    if (fluentdTag.prefix.isEmpty) {
      tag
    } else {
      s"${fluentdTag.prefix}.${tag}"
    }
  }

  protected def toJavaMap(event: Map[String, Any]): java.util.Map[String, AnyRef] = {
    import scala.collection.JavaConverters._
    (for ((k, v) <- event) yield {
      k -> v.asInstanceOf[AnyRef]
    }).asJava
  }
}
