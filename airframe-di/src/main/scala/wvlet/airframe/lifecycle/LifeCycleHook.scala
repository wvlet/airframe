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
package wvlet.airframe.lifecycle

import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

class Injectee(val surface: Surface, val injectee: Any) {
  def canEqual(other: Any): Boolean = other.isInstanceOf[Injectee]
  override def equals(other: Any): Boolean =
    other match {
      case that: Injectee =>
        (that canEqual this) &&
          surface == that.surface &&
          injectee == that.injectee
      case _ => false
    }

  override def hashCode(): Int = {
    val h = 31 * surface.hashCode() + (if (injectee != null) injectee.hashCode() else 0)
    h
  }
}

trait LifeCycleHook {
  def surface: Surface = injectee.surface
  def execute: Unit
  def injectee: Injectee
}

object EventHookHolder {
  def apply[A](surface: Surface, injectee: A, hook: A => Any): EventHookHolder[A] = {
    EventHookHolder(new Injectee(surface, injectee), hook)
  }
}

case class EventHookHolder[A](injectee: Injectee, hook: A => Any) extends LifeCycleHook with LogSupport {
  override def toString: String = s"hook for [$surface]"
  def execute: Unit = {
    hook(injectee.injectee.asInstanceOf[A])
  }
}
