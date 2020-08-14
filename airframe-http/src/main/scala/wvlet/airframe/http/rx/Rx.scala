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
package wvlet.airframe.http.rx

import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

/**
  */
trait Rx[+A] extends LogSupport {
  import Rx._

  def parents: Seq[Rx[_]]
  def withName(name: String): Rx[A] = NamedOp(this, name)

  def map[B](f: A => B): Rx[B]           = MapOp[A, B](this, f)
  def flatMap[B](f: A => Rx[B]): Rx[B]   = FlatMapOp(this, f)
  def filter(f: A => Boolean): Rx[A]     = FilterOp(this, f)
  def withFilter(f: A => Boolean): Rx[A] = FilterOp(this, f)

  /**
    * Combine two Rx objects to form a pair. If one of the objects is updated,
    * it will yield a new pair.
    *
    * This method is useful when you need to monitor multiple Rx objects.
    *
    * Using Zip will be more intuitive than nesting multiple Rx operators
    * like Rx[A].map { x => ... Rx[B].map { ...} }.
    */
  def zip[B](other: Rx[B]): Rx[(A, B)]             = ZipOp(this, other)
  def zip[B, C](b: Rx[B], c: Rx[C]): Rx[(A, B, C)] = Zip3Op(this, b, c)

  def lastOption: RxOption[A] = LastOp(this).toOption

  def toOption[X, A1 >: A](implicit ev: A1 <:< Option[X]): RxOption[X] = RxOptionOp(this.asInstanceOf[Rx[Option[X]]])

  /**
    * Subscribe any change in the upstream, and if a change is detected,
    *  the given subscriber code will be executed.
    *
    * @param subscriber
    * @tparam U
    * @return
    */
  def subscribe[U](subscriber: A => U): Cancelable = run(subscriber)

  def run[U](effect: A => U): Cancelable =
    runInternal {
      case OnNext(v)    => effect(v.asInstanceOf[A])
      case OnError(e)   => throw e
      case OnCompletion =>
      // do nothing
    }

  def runInternal[U](effect: RxEvent => U): Cancelable = {
    RxRunner.runInternal(this)(effect)
  }
}

object Rx extends LogSupport {
  def const[A](v: A): Rx[A] = SingleOp(v)

  def apply[A](v: A): RxVar[A]                        = variable(v)
  def variable[A](v: A): RxVar[A]                     = new RxVar(v)
  def optionVariable[A](v: Option[A]): RxOptionVar[A] = variable(v).toOption
  def option[A](v: Option[A]): RxOption[A]            = RxOptionOp(Rx.const(v))
  val none: RxOption[Nothing]                         = RxOptionOp(Rx.const(None))

  /**
    * Mapping a Scala Future into Rx
    * @param f
    * @param ec
    * @tparam A
    * @return
    */
  def fromFuture[A](f: Future[A])(implicit ec: ExecutionContext): Rx[Option[A]] = {
    val v = Rx.variable[Option[A]](None)
    f.foreach { x => v := Some(x) }
    v
  }

  abstract class UnaryRx[I, A] extends Rx[A] {
    def input: Rx[I]
    override def parents: Seq[Rx[_]] = Seq(input)
  }

  case class SingleOp[A](v: A) extends Rx[A] {
    override def parents: Seq[Rx[_]] = Seq.empty
  }

  case class MapOp[A, B](input: Rx[A], f: A => B)          extends UnaryRx[A, B]
  case class FlatMapOp[A, B](input: Rx[A], f: A => Rx[B])  extends UnaryRx[A, B]
  case class FilterOp[A](input: Rx[A], cond: A => Boolean) extends UnaryRx[A, A]
  case class ZipOp[A, B](left: Rx[A], right: Rx[B]) extends Rx[(A, B)] {
    override def parents: Seq[Rx[_]] = Seq(left, right)
  }
  case class Zip3Op[A, B, C](a: Rx[A], b: Rx[B], c: Rx[C]) extends Rx[(A, B, C)] {
    override def parents: Seq[Rx[_]] = Seq(a, b, c)
  }
  case class LastOp[A](input: Rx[A]) extends Rx[Option[A]] {
    override def parents: Seq[Rx[_]] = Seq(input)
  }
  case class NamedOp[A](input: Rx[A], name: String) extends UnaryRx[A, A] {
    override def toString: String = s"${name}:${input}"
  }

}
