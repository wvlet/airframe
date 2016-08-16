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

import scala.reflect.runtime.{universe=>ru}
import scala.language.experimental.macros

/**
  *
  */
package object inject {

  def inject[A:ru.TypeTag] : A = macro InjectMacros.injectImpl[A]
  def inject[A:ru.TypeTag, D1:ru.TypeTag](factory:D1 => A) : A = macro InjectMacros.inject1Impl[A, D1]
  def inject[A:ru.TypeTag, D1:ru.TypeTag, D2:ru.TypeTag](factory:(D1, D2) => A) : A = macro InjectMacros.inject2Impl[A, D1, D2]
  def inject[A:ru.TypeTag, D1:ru.TypeTag, D2:ru.TypeTag, D3:ru.TypeTag](factory:(D1, D2, D3) => A) : A = macro InjectMacros.inject3Impl[A, D1, D2, D3]
  def inject[A:ru.TypeTag, D1:ru.TypeTag, D2:ru.TypeTag, D3:ru.TypeTag, D4:ru.TypeTag](factory:(D1, D2, D3, D4) => A) : A = macro InjectMacros.inject4Impl[A, D1, D2, D3, D4]
  def inject[A:ru.TypeTag, D1:ru.TypeTag, D2:ru.TypeTag, D3:ru.TypeTag, D4:ru.TypeTag, D5:ru.TypeTag](factory:(D1, D2, D3, D4, D5) => A) : A = macro InjectMacros.inject5Impl[A, D1, D2, D3, D4, D5]

}


