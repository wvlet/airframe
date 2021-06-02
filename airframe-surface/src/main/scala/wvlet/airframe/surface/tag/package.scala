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

import scala.language.implicitConversions

/**
  * This code is from com.softwaremill.tagging
  *
  * Tag instances with arbitrary types. The tags are usually empty `trait`s. Tags have no runtime overhead and are only
  * used at compile-time for additional type safety.
  *
  * For example:
  *
  * {{{
  *   class Berry()
  *
  *   trait Black
  *   trait Blue
  *
  *   val berry = new Berry()
  *   val blackBerry: Berry @@ Black = berry.taggedWith[Black]
  *   val blueBerry: Berry @@ Blue = berry.taggedWith[Blue]
  *
  *   // compile error: val anotherBlackBerry: Berry @@ Black = blueBerry
  * }}}
  *
  * Original idea by Miles Sabin, see: https://gist.github.com/milessabin/89c9b47a91017973a35f
  */
package object tag {
  type Tag[+U]       = { type Tag <: U }
  type @@[T, +U]     = T with Tag[U]
  type Tagged[T, +U] = T with Tag[U]
  implicit class Tagger[T](t: T) {
    def taggedWith[U]: T @@ U = t.asInstanceOf[T @@ U]
  }

  implicit def toTaggedType[A, Tag](obj: A): A @@ Tag = obj.taggedWith[Tag]
}
