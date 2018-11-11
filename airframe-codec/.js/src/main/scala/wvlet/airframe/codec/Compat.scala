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
import wvlet.airframe.surface.Surface

/**
  *
  */
object Compat {

  def codecFinder: CodecFinder                      = JSCodecFinger
  def platformCodecs: Map[Surface, MessageCodec[_]] = Map.empty

  object JSCodecFinger extends CodecFinder {
    override def findCodec(factory: MessageCodecFactory,
                           seenSet: Set[Surface]): PartialFunction[Surface, MessageCodec[_]] = {
      case other => throw new UnsupportedOperationException(s"${other}")
    }
  }
}
