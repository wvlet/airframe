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
import wvlet.airframe.rx.Rx.{FlatMapOp, MapOp, RecoverOp, RecoverWithOp}

/**
  */
trait RxOption[+A] extends RxBase[Option[A]] {
  protected def in: Rx[Option[A]]

  override def toRx: Rx[Option[A]] = transform {
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
    transformRx[B] {
      case Some(x) => f(x).map(Some(_))
      case None    => Rx.single(None)
    }
  }

  def transform[B](f: Option[A] => B): Rx[B] = {
    MapOp(
      in,
      { x: Option[A] =>
        f(x)
      }
    )
  }

  def transformRx[B](f: Option[A] => Rx[Option[B]]): RxOption[B] = {
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

  def getOrElse[A1 >: A](default: => A1): Rx[A1] = {
    transform {
      case Some(v) => v
      case None    => default
    }
  }

  def orElse[A1 >: A](default: => Option[A1]): RxOption[A1] = {
    transformOption(_.orElse(default))
  }

  def filter(f: A => Boolean): RxOption[A] = {
    RxOptionOp(
      in.map {
        case Some(x) if f(x) => Some(x)
        case _               => None
      }
    )
  }

  def withFilter(f: A => Boolean): RxOption[A] = filter(f)
}

case class RxOptionOp[+A](override protected val in: Rx[Option[A]]) extends RxOption[A]

/**
  * RxVar implementation for Option[A] type values
  * @tparam A
  */
class RxOptionVar[A](variable: RxVar[Option[A]]) extends RxOption[A] with RxVarOps[Option[A]] {
  override def toString: String            = s"RxOptionVar(${variable.get})"
  override protected def in: Rx[Option[A]] = variable

  override def get: Option[A] = variable.get
  override def foreach[U](f: Option[A] => U): Cancelable = {
    variable.foreach(f)
  }
  override def update(updater: Option[A] => Option[A], force: Boolean = false): Unit = {
    variable.update(updater, force)
  }
}
