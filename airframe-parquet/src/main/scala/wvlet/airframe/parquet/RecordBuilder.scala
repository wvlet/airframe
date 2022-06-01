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
package wvlet.airframe.parquet

import wvlet.log.LogSupport

import java.util
import scala.collection.immutable.ListMap

trait RecordBuilder {
  def add(fieldName: String, v: Any): Unit
  def toMap: Map[String, Any]
  def clear(): Unit
}

class RecordBuilderImpl extends RecordBuilder with LogSupport {
  // Using ListMap for preserving the field order
  private var holder = ListMap.empty[String, Any]

  def add(fieldName: String, v: Any): Unit = {
    holder.get(fieldName) match {
      case None =>
        holder += fieldName -> v
      case Some(prev) =>
        prev match {
          case seq: Seq[_] =>
            // Note: Tail insertion for Seq is inefficient O(N). We may need to optimize it later
            holder += fieldName -> (seq.asInstanceOf[Seq[Any]] :+ v)
          case _ =>
            holder += fieldName -> Seq(prev, v)
        }
    }
  }

  def toMap: Map[String, Any] = {
    holder
  }
  def clear(): Unit = {
    holder = ListMap.empty[String, Any]
  }
}

object RecordBuilder {
  def newBuilder: RecordBuilder = new RecordBuilderImpl
}
