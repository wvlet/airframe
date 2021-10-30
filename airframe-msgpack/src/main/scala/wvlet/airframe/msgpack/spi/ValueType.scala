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
package wvlet.airframe.msgpack.spi;

sealed abstract class ValueType(val isNumber: Boolean, val isRaw: Boolean) {
  def name: String = toString
}

/**
  * Representation of MessagePack value types.
  */
object ValueType {
  case object NIL       extends ValueType(false, false)
  case object BOOLEAN   extends ValueType(false, false)
  case object INTEGER   extends ValueType(true, false)
  case object FLOAT     extends ValueType(true, false)
  case object STRING    extends ValueType(false, true)
  case object BINARY    extends ValueType(false, true)
  case object ARRAY     extends ValueType(false, false)
  case object MAP       extends ValueType(false, false)
  case object EXTENSION extends ValueType(false, false)

  // abstract class ExtValueType
  // Timestamp value should be reported as a first-class value?
  // TODO: But we cannot determine the value type without reading the second or more bytes
//  sealed trait ExtValueType
//  object Ext {
//    case object TIMESTAMP extends ExtValueType
//  }
}
