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

package wvlet.surface

trait MethodSurface {
  def mod: Int
  def owner: Surface
  def name: String
  def args: Seq[MethodParameter]
  def returnType: Surface

  def isPublic: Boolean = (mod & MethodModifier.PUBLIC) != 0
  def isPrivate: Boolean = (mod & MethodModifier.PRIVATE) != 0
  def isProtected: Boolean = (mod & MethodModifier.PROTECTED) != 0
  def isStatic: Boolean = (mod & MethodModifier.STATIC) != 0
  def isFinal: Boolean = (mod & MethodModifier.FINAL) != 0
  def isAbstract: Boolean = (mod & MethodModifier.ABSTRACT) != 0
}

case class ClassMethodSurface(mod: Int, owner: Surface, name: String, returnType: Surface, args: Seq[MethodParameter]) extends MethodSurface


