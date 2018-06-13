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
package wvlet.airframe.control

/**
  * An abstraction of code execution
  */
// Using +A to make it possible to upper-cast (including Nothing, which is a super-class of all classes)
trait Code[+A] {
  import Code._
  def deferred[A1 >: A](a: => A1): Code[A1]                         = new Lazy(a)
  def pure[A1 >: A](a: A1): Code[A1]                                = Now(a)
  def now[A1 >: A](a: A1): Code[A1]                                 = Now(a)
  def map[B](f: A => B): Code[B]                                    = Map(f)
  def flatMap[B](f: A => Code[B]): Code[B]                          = FlatMap(f)
  def rescue[A1 >: A](handler: Throwable => A1): Code[A1]           = Rescue(handler)
  def rescueWith[A1 >: A](handler: Throwable => Code[A1]): Code[A1] = RescueWith(handler)

  def andThen[B](code: Code[B]): Code[B] = AndThen(this, code)
}

object Code {
  def deffered[A](f: => A)     = new Lazy(f)
  def raiseError(e: Throwable) = Error(e)
  def sleepMillis(millis: Int) = Sleep(millis)

  class Lazy[A](f: => A)                                  extends Code[A]
  case class Now[A](a: A)                                 extends Code[A]
  case class Map[A, B](f: A => B)                         extends Code[B]
  case class FlatMap[A, B](f: A => Code[B])               extends Code[B]
  case class Rescue[A](handler: Throwable => A)           extends Code[A]
  case class RescueWith[A](handler: Throwable => Code[A]) extends Code[A]
  case class Error(e: Throwable)                          extends Code[Nothing]
  case class Sleep(millis: Int)                           extends Code[Nothing]

  case class AndThen[A, B](prev: Code[A], next: Code[B]) extends Code[B]
}
