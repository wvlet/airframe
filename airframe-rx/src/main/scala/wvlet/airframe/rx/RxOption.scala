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
package wvlet.airframe.rx
import wvlet.airframe.rx.Rx.{CacheOp, FlatMapOp, MapOp, RecoverOp, RecoverWithOp}

import java.util.concurrent.TimeUnit

/**
  */
trait RxOption[+A] extends Rx[Option[A]] {
  protected def in: RxStream[Option[A]]

  override def toRxStream: RxStream[Option[A]] = transform {
    case Some(x) => Some(x)
    case None    => None
  }

  def map[B](f: A => B): RxOption[B] = {
    transformOption {
      case Some(x) => Some(f(x))
      case None    => None
    }
  }

  def flatMap[B](f: A => Rx[B]): RxOption[B] = {
    RxOptionOp[B](
      transformRx {
        case Some(x) =>
          f(x).toRxStream.map(Option(_))
        case None => Rx.none
      }
    )
  }

  def transform[B](f: Option[A] => B): RxStream[B] = {
    MapOp(
      in,
      { x: Option[A] =>
        f(x)
      }
    )
  }

  def transformRx[B](f: Option[A] => Rx[B]): RxStream[B] = {
    in.flatMap(f)
  }

  def transformRxOption[B](f: Option[A] => RxOption[B]): RxOption[B] = {
    RxOptionOp[B](
      FlatMapOp(
        in,
        { x: Option[A] =>
          f(x)
        }
      )
    )
  }

  def transformOption[B](f: Option[A] => Option[B]): RxOption[B] = {
    RxOptionOp[B](
      MapOp(
        in,
        { x: Option[A] =>
          f(x)
        }
      )
    )
  }

  def getOrElse[A1 >: A](default: => A1): RxStream[A1] = {
    transform {
      case Some(v) => v
      case None    => default
    }
  }

  def getOrElseRx[A1 >: A](default: => Rx[A1]): RxStream[A1] = {
    transformRx {
      case Some(v) => Rx.single(v.asInstanceOf[A1])
      case None    => default
    }
  }

  def orElse[A1 >: A](default: => Option[A1]): RxOption[A1] = {
    transformOption(_.orElse(default))
  }

  def filter(f: A => Boolean): RxOption[A]     = transformOption(_.filter(f))
  def withFilter(f: A => Boolean): RxOption[A] = filter(f)

  def cache[A1 >: A]: RxOptionCache[A1] = RxOptionCacheOp(CacheOp(this))
}

/**
  * An interface for enriching RxOption[A] with caching capability
  * @tparam A
  */
trait RxOptionCache[A] extends RxOption[A] {
  def expireAfterWrite(time: Long, unit: TimeUnit): RxOptionCache[A]
  def refreshAfterWrite(time: Long, unit: TimeUnit): RxOptionCache[A]
  def withTicker(ticker: Ticker): RxOptionCache[A]
}

case class RxOptionOp[+A](override protected val in: RxStream[Option[A]]) extends RxOption[A] {
  override def parents: Seq[Rx[_]] = Seq(in)
}

/**
  * RxVar implementation for Option[A] type values
  * @tparam A
  */
class RxOptionVar[A](variable: RxVar[Option[A]]) extends RxOption[A] with RxVarOps[Option[A]] {
  override def toString: String                  = s"RxOptionVar(${variable.get})"
  override protected def in: RxStream[Option[A]] = variable
  override def parents: Seq[Rx[_]]               = Seq(in)

  override def get: Option[A] = variable.get
  override def foreach[U](f: Option[A] => U): Cancelable = {
    variable.foreach(f)
  }
  override def update(updater: Option[A] => Option[A], force: Boolean = false): Unit = {
    variable.update(updater, force)
  }

}

case class RxOptionCacheOp[A](input: RxStreamCache[Option[A]]) extends RxOptionCache[A] {
  override protected def in: RxStream[Option[A]] = input.toRxStream
  override def parents: Seq[Rx[_]]               = input.parents

  override def expireAfterWrite(time: Long, unit: TimeUnit): RxOptionCache[A] =
    this.copy(input = input.expireAfterWrite(time, unit))

  override def refreshAfterWrite(time: Long, unit: TimeUnit): RxOptionCache[A] = {
    this.copy(input = input.refreshAfterWrite(time, unit))
  }

  override def withTicker(ticker: Ticker): RxOptionCache[A] = {
    this.copy(input = input.withTicker(ticker))
  }
}
