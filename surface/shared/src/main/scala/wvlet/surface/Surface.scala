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

trait Surface extends Serializable {
  def rawType: Class[_]
  def typeArgs: Seq[Surface]
  def params: Seq[Parameter]
  def name: String
  def fullName: String

  def dealias: Surface = this

  def isOption: Boolean
  def isAlias: Boolean
  def isPrimitive: Boolean

  def objectFactory: Option[ObjectFactory] = None
}

trait Parameter {
  def index: Int
  def name: String

  /**
    * Surface for representing this parameter type
    */
  def surface: Surface

  /**
    * Get this parameter value from a given object x
    */
  def get(x: Any): Any

  /**
    * Get the default value of this parameter.
    * For example the default value of x in class A(x:Int = 10) is 10
    *
    * @return
    */
  def getDefaultValue: Option[Any]
}


/**
  *
  */
trait ObjectFactory extends Serializable {
  def newInstance(args: Seq[Any]): Any
}

case class MethodRef(owner: Class[_], name: String, paramTypes: Seq[Class[_]], isConstructor: Boolean)

trait MethodParameter extends Parameter {
  def method: MethodRef
}
