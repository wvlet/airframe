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
package wvlet.airframe.msgpack.spi

sealed trait ValueType

object ValueType {
  case object NIL       extends ValueType
  case object BOOLEAN   extends ValueType
  case object INTEGER   extends ValueType
  case object FLOAT     extends ValueType
  case object STRING    extends ValueType
  case object BINARY    extends ValueType
  case object ARRAY     extends ValueType
  case object MAP       extends ValueType
  case object EXTENSION extends ValueType
}
