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
import wvlet.airframe.rx.Rx.{FlatMapOp, MapOp}

/**
  * An wrapper of Rx[A] for Option[A] type values
  */
private[rx] trait RxOption[+A] extends Rx[A] {
  protected def in: Rx[Option[A]]

  override def parents: Seq[Rx[_]]                 = Seq(in)
  override def withName(name: String): RxOption[A] = RxOptionOp(in.withName(name))

  override def map[B](f: A => B): RxOption[B] = {
    RxOptionOp(
      MapOp(
        in,
        { x: Option[A] =>
          x.map(f)
        }
      )
    )
  }
  override def flatMap[B](f: A => Rx[B]): RxOption[B] = {
    RxOptionOp[B](
      FlatMapOp(
        in,
        { x: Option[A] =>
          x match {
            case Some(v) =>
              f(v).map(Some(_))
            case None =>
              Rx.none
          }
        }
      )
    )
  }

  def getOrElse[A1 >: A](default: => A1): Rx[A1] = {
    MapOp(
      in,
      { x: Option[A] =>
        x.getOrElse(default)
      }
    )
  }

  def orElse[A1 >: A](default: => Option[A1]): RxOption[A1] = {
    RxOptionOp[A1](
      MapOp(
        in,
        { x: Option[A] =>
          x.orElse(default)
        }
      )
    )
  }

  override def filter(f: A => Boolean): RxOption[A] = {
    RxOptionOp(
      in.map {
        case Some(x) if f(x) => Some(x)
        case _               => None
      }
    )
  }

  override def withFilter(f: A => Boolean): RxOption[A] = filter(f)
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
