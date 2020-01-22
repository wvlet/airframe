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
import scala.util.Try

/**
  *
  */
trait Cancelable {
  def cancel: Unit = {}
}

object Cancelable {
  val empty: Cancelable = new Cancelable {}

  def apply(canceller: () => Unit): Cancelable = new Cancelable {
    override def cancel: Unit = canceller()
  }

  def merge(c1: Cancelable, c2: Cancelable): Cancelable = {
    if (c1 == Cancelable.empty) {
      c2
    } else if (c2 == Cancelable.empty) {
      c1
    } else {
      Cancelable { () =>
        try {
          c1.cancel
        } finally {
          c2.cancel
        }
      }
    }
  }

  def merge(lst: Seq[Cancelable]): Cancelable = {
    lst.size match {
      case 1 => lst.head
      case _ =>
        val nonEmpty = lst.filter(_ != Cancelable.empty)
        if (nonEmpty.isEmpty) {
          Cancelable.empty
        } else {
          Cancelable { () =>
            nonEmpty.map { c =>
              Try(c.cancel)
            }
          }
        }
    }
  }
}
