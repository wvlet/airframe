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
  * Representation of MessagePack types.
  * <p>
  * MessagePack uses hierarchical type system. Integer and Float are subypte of Number, Thus {@link #isNumberType()}
  * returns true if type is Integer or Float. String and Binary are subtype of Raw. Thus {@link #isRawType()} returns
  * true if type is String or Binary.
  *
  * TODO: Convert this into Scala
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

  // Timestamp value should be reported as a first-class value?
  // TODO: But we cannot determin the value type without reading the second or more bytes
  // case object TIMESTAMP extends ValueType(false, false)
}
