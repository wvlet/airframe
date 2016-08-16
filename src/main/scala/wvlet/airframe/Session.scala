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
package wvlet.airframe

import wvlet.obj.ObjectType

import scala.reflect.runtime.{universe => ru}
import scala.language.experimental.macros

/**
  * Context tracks the dependencies of objects and use them to instantiate objects
  */
trait Session {

  /**
'    * Creates an instance of the given type A.
    *
    * @tparam A
    * @return object
    */
  def get[A: ru.WeakTypeTag]: A
  def getOrElseUpdate[A: ru.WeakTypeTag](obj: => A): A
  def build[A: ru.WeakTypeTag]: A = macro AirframeMacros.buildImpl[A]

  def register[A: ru.WeakTypeTag](obj:A) : A
}


trait SessionListener {
  def afterInjection(t: ObjectType, injectee: Any)
}





