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
package wvlet.frame

import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}
import scala.language.experimental.macros

/**
  *
  */
object Frame {
  def of[A:ru.TypeTag] : Frame[A] = macro FrameMacros.of[A]
}

case class Parameter(name:String, frame:Frame[_])

case class Frame[A](cl:Class[A], params:Seq[Parameter]=Seq.empty) {
  override def toString = s"Frame[${cl.getName}](${params.mkString(",")})"
}
