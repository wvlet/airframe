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
package wvlet.airframe.surface

/**
  */
object MethodModifier {
  val PUBLIC       = 0x00000001
  val PRIVATE      = 0x00000002
  val PROTECTED    = 0x00000004
  val STATIC       = 0x00000008
  val FINAL        = 0x00000010
  val SYNCHRONIZED = 0x00000020
  val VOLATILE     = 0x00000040
  val TRANSIENT    = 0x00000080
  val NATIVE       = 0x00000100
  val INTERFACE    = 0x00000200
  val ABSTRACT     = 0x00000400
  val STRICT       = 0x00000800
}
