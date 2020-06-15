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

package wvlet.airframe.surface

import scala.language.existentials
import scala.language.experimental.macros

object Surface {
  def of[A]: Surface = macro SurfaceMacros.surfaceOf[A]
  def methodsOf[A]: Seq[MethodSurface] = macro SurfaceMacros.methodSurfaceOf[A]
}

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
  def isSeq: Boolean = classOf[Seq[_]].isAssignableFrom(rawType)

  def objectFactory: Option[ObjectFactory] = None
}

sealed trait ParameterBase extends Serializable {
  def name: String
  def surface: Surface

  def call(obj: Any, x: Any*): Any
}

trait Parameter extends ParameterBase {
  def index: Int
  def name: String

  /**
    * Surface for representing this parameter type
    */
  def surface: Surface

  /**
    * Returns true if this parameter has @required annotation
    */
  def isRequired: Boolean

  /**
    * Returns true if this parameter has @required annotation
    */
  def isSecret: Boolean

  /**
    * Get this parameter value from a given object x
    */
  def get(x: Any): Any

  override def call(obj: Any, x: Any*): Any = get(obj)

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

  /**
    * Method owner instance is necessary to find by-name parameter default values
    * @param methodOwner
    * @return
    */
  def getMethodArgDefaultValue(methodOwner: Any): Option[Any] = getDefaultValue
}

trait MethodSurface extends ParameterBase {
  def mod: Int
  def owner: Surface
  def name: String
  def args: Seq[MethodParameter]
  def surface: Surface = returnType
  def returnType: Surface

  def isPublic: Boolean    = (mod & MethodModifier.PUBLIC) != 0
  def isPrivate: Boolean   = (mod & MethodModifier.PRIVATE) != 0
  def isProtected: Boolean = (mod & MethodModifier.PROTECTED) != 0
  def isStatic: Boolean    = (mod & MethodModifier.STATIC) != 0
  def isFinal: Boolean     = (mod & MethodModifier.FINAL) != 0
  def isAbstract: Boolean  = (mod & MethodModifier.ABSTRACT) != 0
}
