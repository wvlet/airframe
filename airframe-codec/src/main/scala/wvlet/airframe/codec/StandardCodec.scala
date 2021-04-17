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
package wvlet.airframe.codec

import java.util.UUID
import wvlet.airframe.surface.Surface
import wvlet.airframe.ulid.ULID

/**
  * Standard codec collection
  */
object StandardCodec {
  val javaClassCodec = Map(
    Surface.of[Throwable]         -> ThrowableCodec,
    Surface.of[Exception]         -> ThrowableCodec,
    Surface.of[java.time.Instant] -> JavaInstantTimeCodec,
    Surface.of[UUID]              -> UUIDCodec,
    Surface.of[ULID]              -> ULIDCodec
  )

  val standardCodec: Map[Surface, MessageCodec[_]] =
    PrimitiveCodec.primitiveCodec ++ PrimitiveCodec.primitiveArrayCodec ++ javaClassCodec
}
