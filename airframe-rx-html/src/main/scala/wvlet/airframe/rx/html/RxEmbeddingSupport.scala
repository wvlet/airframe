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
package wvlet.airframe.rx.html

import wvlet.airframe.rx.{Rx, RxOption}
import wvlet.airframe.rx.html.compat
import wvlet.log.LogSupport

import scala.annotation.implicitNotFound
import scala.language.higherKinds
import scala.language.implicitConversions

trait RxEmbeddingSupport {
  @implicitNotFound(msg = "Unsupported type as an attribute value")
  trait EmbeddableAttribute[X]

  object EmbeddableAttribute {
    type EA[A] = EmbeddableAttribute[A]
    @inline implicit def embedNone: EA[None.type]                            = null
    @inline implicit def embedBoolean: EA[Boolean]                           = null
    @inline implicit def embedInt: EA[Int]                                   = null
    @inline implicit def embedLong: EA[Long]                                 = null
    @inline implicit def embedString: EA[String]                             = null
    @inline implicit def embedFloat: EA[Float]                               = null
    @inline implicit def embedDouble: EA[Double]                             = null
    @inline implicit def embedF0[U]: EA[() => U]                             = null
    @inline implicit def embedF1[I, U]: EA[I => U]                           = null
    @inline implicit def embedOption[C[x] <: Option[x], A: EA]: EA[C[A]]     = null
    @inline implicit def embedRx[C[x] <: Rx[x], A: EA]: EA[C[A]]             = null
    @inline implicit def embedRxOption[C[x] <: RxOption[x], A: EA]: EA[C[A]] = null
    @inline implicit def embedSeq[C[x] <: Iterable[x], A: EA]: EA[C[A]]      = null
  }

  @implicitNotFound(msg = "Unsupported type as an HtmlNode")
  trait EmbeddableNode[A]

  object EmbeddableNode extends compat.PlatformEmbeddableNode {
    type EN[A] = EmbeddableNode[A]
    @inline implicit def embedNil: EN[Nil.type]                              = null
    @inline implicit def embedNone: EN[None.type]                            = null
    @inline implicit def embedBoolean: EN[Boolean]                           = null
    @inline implicit def embedInt: EN[Int]                                   = null
    @inline implicit def embedChar: EN[Char]                                 = null
    @inline implicit def embedShort: EN[Short]                               = null
    @inline implicit def embedByte: EN[Byte]                                 = null
    @inline implicit def embedLong: EN[Long]                                 = null
    @inline implicit def embedFloat: EN[Float]                               = null
    @inline implicit def embedDouble: EN[Double]                             = null
    @inline implicit def embedString: EN[String]                             = null
    @inline implicit def embedHtmlNode[A <: HtmlNode]: EN[A]                 = null
    @inline implicit def embedRx[C[x] <: Rx[x], A: EN]: EN[C[A]]             = null
    @inline implicit def embedRxOption[C[x] <: RxOption[x], A: EN]: EN[C[A]] = null
    @inline implicit def embedSeq[C[x] <: Iterable[x], A: EN]: EN[C[A]]      = null
    @inline implicit def embedOption[C[x] <: Option[x], A: EN]: EN[C[A]]     = null
  }

  /**
    * Holder for embedding various types as tag contents
    *
    * @param v
    */
  case class Embedded(v: Any) extends RxElement with LogSupport {
    override def render: RxElement = {
      warn(s"render is called for ${v}")
      ???
    }
  }

  implicit def embedAsNode[A: EmbeddableNode](v: A): RxElement = Embedded(v)
}
