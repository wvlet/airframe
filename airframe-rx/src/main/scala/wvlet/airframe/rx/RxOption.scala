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
import wvlet.airframe.rx.Rx.{
  CacheOp,
  FlatMapOp,
  Join3Op,
  Join4Op,
  Join5Op,
  JoinOp,
  MapOp,
  RecoverOp,
  RecoverWithOp,
  Zip3Op,
  Zip4Op,
  Zip5Op,
  ZipOp
}

import java.util.concurrent.TimeUnit

/**
  */
trait RxOption[+A] extends RxOps[Option[A]] {
  protected def in: Rx[Option[A]]

  def toRx: Rx[Option[A]] = transform {
    case Some(x) => Some(x)
    case None    => None
  }

  def map[B](f: A => B): RxOption[B] = {
    transformOption {
      case Some(x) => Some(f(x))
      case None    => None
    }
  }

  def flatMap[B](f: A => RxOps[B]): RxOption[B] = {
    RxOptionOp[B](
      transformRx {
        case Some(x) =>
          f(x).toRx.map(Option(_))
        case None =>
          Rx.none.toRx
      }
    )
  }

  def join[B](b: RxOps[B]): Rx[(Option[A], B)]                                       = JoinOp(this, b)
  def join[B, C](b: RxOps[B], c: RxOps[C]): Rx[(Option[A], B, C)]                    = Join3Op(this, b, c)
  def join[B, C, D](b: RxOps[B], c: RxOps[C], d: RxOps[D]): Rx[(Option[A], B, C, D)] = Join4Op(this, b, c, d)
  def join[B, C, D, E](b: RxOps[B], c: RxOps[C], d: RxOps[D], e: RxOps[E]): Rx[(Option[A], B, C, D, E)] =
    Join5Op(this, b, c, d, e)
  def zip[B](b: RxOps[B]): Rx[(Option[A], B)]                                       = ZipOp(this, b)
  def zip[B, C](b: RxOps[B], c: RxOps[C]): Rx[(Option[A], B, C)]                    = Zip3Op(this, b, c)
  def zip[B, C, D](b: RxOps[B], c: RxOps[C], d: RxOps[D]): Rx[(Option[A], B, C, D)] = Zip4Op(this, b, c, d)
  def zip[B, C, D, E](b: RxOps[B], c: RxOps[C], d: RxOps[D], e: RxOps[E]): Rx[(Option[A], B, C, D, E)] =
    Zip5Op(this, b, c, d, e)

  def transform[B](f: Option[A] => B): Rx[B] = {
    MapOp(
      in,
      { (x: Option[A]) =>
        f(x)
      }
    )
  }

  def transformRx[B](f: Option[A] => RxOps[B]): Rx[B] = {
    in.flatMap(f)
  }

  def transformRxOption[B](f: Option[A] => RxOption[B]): RxOption[B] = {
    RxOptionOp[B](
      FlatMapOp(
        in,
        { (x: Option[A]) =>
          f(x).toRx
        }
      )
    )
  }

  def transformOption[B](f: Option[A] => Option[B]): RxOption[B] = {
    RxOptionOp[B](
      MapOp(
        in,
        { (x: Option[A]) =>
          f(x)
        }
      )
    )
  }

  def getOrElse[A1 >: A](default: => A1): Rx[A1] = {
    transform {
      case Some(v) => v
      case None    => default
    }
  }

  def getOrElseRx[A1 >: A](default: => RxOps[A1]): Rx[A1] = {
    transformRx {
      case Some(v) => Rx.single(v.asInstanceOf[A1])
      case None    => default
    }
  }

  def orElse[A1 >: A](default: => Option[A1]): RxOption[A1] = {
    transformOption(_.orElse(default))
  }

  def filter(f: A => Boolean): RxOption[A] = transformOption(_.filter(f))

  /**
    * An alias for filter
    * @param cond
    * @return
    */
  def when(cond: A => Boolean): RxOption[A] = filter(cond)

  def withFilter(f: A => Boolean): RxOption[A] = filter(f)

  def cache[A1 >: A]: RxOptionCache[A1] = RxOptionCacheOp(CacheOp(this.toRx))

}

/**
  * An interface for enriching RxOption[A] with caching capability
  * @tparam A
  */
trait RxOptionCache[A] extends RxOption[A] {
  def getCurrent: Option[A]
  def expireAfterWrite(time: Long, unit: TimeUnit): RxOptionCache[A]
  def withTicker(ticker: Ticker): RxOptionCache[A]
}

case class RxOptionOp[+A](override protected val in: Rx[Option[A]]) extends RxOption[A] {
  override def parents: Seq[Rx[_]] = Seq(in)
}

/**
  * RxVar implementation for Option[A] type values
  * @tparam A
  */
class RxOptionVar[A](variable: RxVar[Option[A]]) extends RxOption[A] with RxVarOps[Option[A]] {
  override def toString: String            = s"RxOptionVar(${variable.get})"
  override protected def in: Rx[Option[A]] = variable
  override def parents: Seq[RxOps[_]]      = Seq(in)

  override def get: Option[A] = variable.get
  override def foreach[U](f: Option[A] => U): Cancelable = {
    variable.foreach(f)
  }
  override def foreachEvent[U](effect: RxEvent => U): Cancelable = {
    variable.foreachEvent(effect)
  }
  override def update(updater: Option[A] => Option[A], force: Boolean = false): Unit = {
    variable.update(updater, force)
  }

  override def setException(e: Throwable): Unit = {
    variable.setException(e)
  }

  override def stop(): Unit = {
    variable.stop()
  }
}

case class RxOptionCacheOp[A](input: RxCache[Option[A]]) extends RxOptionCache[A] {
  override def getCurrent: Option[A]       = input.getCurrent.flatten
  override protected def in: Rx[Option[A]] = input.toRx
  override def parents: Seq[RxOps[_]]      = input.parents

  override def expireAfterWrite(time: Long, unit: TimeUnit): RxOptionCache[A] =
    this.copy(input = input.expireAfterWrite(time, unit))

  override def withTicker(ticker: Ticker): RxOptionCache[A] = {
    this.copy(input = input.withTicker(ticker))
  }
}
