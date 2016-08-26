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
package wvlet

import wvlet.airframe.AirframeMacros._
import wvlet.log.LogSupport

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}

/**
  *
  */
package object airframe {
  /**
    * The entry point to create a new design beginning from a blanc design
    * <code>
    * import wvlet.airframe._
    *
    * val d = design.bind[X]
    * </code>
    */
  def newDesign: Design = Design.blanc

  def bind[A: ru.TypeTag]: A = macro bindImpl[A]
  def bind[A: ru.TypeTag](factory: => A): A = macro bind0Impl[A]
  def bind[A: ru.TypeTag, D1: ru.TypeTag](factory: D1 => A): A = macro bind1Impl[A, D1]
  def bind[A: ru.TypeTag, D1: ru.TypeTag, D2: ru.TypeTag](factory: (D1, D2) => A): A = macro bind2Impl[A, D1, D2]
  def bind[A: ru.TypeTag, D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag](factory: (D1, D2, D3) => A): A = macro bind3Impl[A, D1, D2, D3]
  def bind[A: ru.TypeTag, D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag]
  (factory: (D1, D2, D3, D4) => A): A = macro bind4Impl[A, D1, D2, D3, D4]
  def bind[A: ru.TypeTag, D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag, D5: ru.TypeTag]
  (factory: (D1, D2, D3, D4, D5) => A): A = macro bind5Impl[A, D1, D2, D3, D4, D5]

  private[airframe] val DO_NOTHING = { a: Any => }

  /**
    * bind[A].withLifeCycle(init = ..., start = ..., shutdown = ...)
    */
  implicit class LifeCycleSupport[A](val dep: A) extends LogSupport {
    def withLifeCycle: LifeCycleBinder[A] = macro addLifeCycle
  }

  class LifeCycleBinder[A](dep: A, session: Session) {
    def apply(init: A => Unit = DO_NOTHING, start: A => Unit = DO_NOTHING,
              shutdown: A => Unit = DO_NOTHING): A = {
      if (init != DO_NOTHING) {
        session.lifeCycleManager.addInitHook(EventHookHolder(dep, init))
      }
      if (start != DO_NOTHING) {
        session.lifeCycleManager.addStartHook(EventHookHolder(dep, start))
      }
      if (shutdown != DO_NOTHING) {
        session.lifeCycleManager.addShutdownHook(EventHookHolder(dep, shutdown))
      }
      dep
    }
  }

  import wvlet.obj.tag._

  // Automatically add tag
  implicit def toTaggedType[A, Tag](obj: A): A @@ Tag = obj.taggedWith[Tag]
}
