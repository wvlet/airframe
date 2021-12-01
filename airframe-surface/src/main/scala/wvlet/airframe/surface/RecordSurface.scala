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

/**
  * Surface for custom record types
  */
case class RecordSurface(
    name: String,
    fullName: String,
    rawType: Class[_] = classOf[Any],
    typeArgs: Seq[Surface] = Seq.empty,
    params: Seq[Parameter] = Seq.empty,
    isRequired: Boolean = false,
    isSecret: Boolean = false,
    isOption: Boolean = false,
    isPrimitive: Boolean = false,
    override val isSeq: Boolean = false,
    override val isArray: Boolean = false,
    override val isMap: Boolean = false,
    override val objectFactory: Option[ObjectFactory] = None
) extends Surface {
  override def dealias: Surface = this

  override def isAlias: Boolean = false

  def withTypeArgs(newTypeArgs: Seq[Surface]): RecordSurface = this.copy(typeArgs = newTypeArgs)
  def withRawType(cls: Class[_]): RecordSurface              = this.copy(rawType = cls)
  def withParams(newParams: Seq[Parameter]): RecordSurface   = this.copy(params = newParams)
  def addParam(newParam: Parameter): RecordSurface = {
    require(newParam.index == params.length, s"index must be ${params.length}: ${newParam.index}")
    this.copy(params = params :+ newParam)
  }
  def withObjectFactory(newFactory: ObjectFactory): RecordSurface = this.copy(objectFactory = Some(newFactory))
  def asRequired: RecordSurface                                   = this.copy(isRequired = true)
  def asSecret: RecordSurface                                     = this.copy(isSecret = true)
  def asPrimitive: RecordSurface                                  = this.copy(isPrimitive = true)
  def asOption: RecordSurface                                     = this.copy(isOption = true)
  def asSeq: RecordSurface = {
    require(typeArgs.size == 1, s"typeArgs must have one parameter for Seq: ${typeArgs}")
    this.copy(isSeq = true, isArray = false, isMap = false)
  }
  def asArray: RecordSurface = {
    require(typeArgs.size == 1, s"typeArgs must have one parameter for Array: ${typeArgs}")
    this.copy(isSeq = false, isArray = true, isMap = false)
  }
  def asMap: RecordSurface = {
    require(typeArgs.size == 2, s"typeArgs must have two parameters for Map: ${typeArgs}")
    this.copy(isSeq = false, isArray = false, isMap = true)
  }
}

object RecordSurface {
  def newSurface(fullName: String): RecordSurface = {
    val name = fullName.split("\\.").lastOption.getOrElse(fullName)
    RecordSurface(name = name, fullName = fullName)
  }
}

case class RecordParameter(
    index: Int,
    name: String,
    surface: Surface,
    isRequired: Boolean = false,
    isSecret: Boolean = false,
    defaultValue: Option[Any] = None
) extends Parameter {
  override def get(x: Any): Any             = ???
  override def getDefaultValue: Option[Any] = defaultValue
}
