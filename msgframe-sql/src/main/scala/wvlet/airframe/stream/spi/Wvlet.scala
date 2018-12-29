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
package wvlet.airframe.stream.spi

/**
  *
  */
trait Wvlet[A] {
  import Wvlet._
  def map[B](f: A => B): Wvlet[B]            = MapOp(this, f)
  def flatMap[B](f: A => Wvlet[B]): Wvlet[B] = FlatMapOp(this, f)
  def filter(cond: A => Boolean): Wvlet[A]   = FilterOp(this, cond)
}

object Wvlet {
  case class MapOp[A, B](in: Wvlet[A], f: A => B)            extends Wvlet[B]
  case class FlatMapOp[A, B](in: Wvlet[A], f: A => Wvlet[B]) extends Wvlet[B]
  case class FilterOp[A](in: Wvlet[A], cond: A => Boolean)   extends Wvlet[A]
}
