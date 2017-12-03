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
package wvlet.airframe.msgpack.spi

/**
  *
  */
trait Value {
  def toJson: String
  def valueType: ValueType
  def toImmutable: Value

  def writeTo(packer: Packer)
//  def isNil: Boolean
//  def isBoolean: Boolean
//  def isNumber: Boolean
//  def isInteger: Boolean
//  def isFloat: Boolean
//  def isRaw: Boolean
//  def isBinary: Boolean
//  def isString: Boolean
//  def isArray: Boolean
//  def isMap: Boolean
}

object Value {
  case object NilValue extends Value {
    def toJson             = "null"
    def valueType          = ValueType.NIL
    def toImmutable: Value = this
    def writeTo(packer: Packer): Unit = {
      packer.packNil
    }
  }
  case class BooleanValue(v:Boolean) extends Value {
    def toJson             = if(v) "true" else "false"
    def valueType          = ValueType.NIL
    def toImmutable: Value = this
    def writeTo(packer: Packer): Unit = {
      packer.packBoolean(v)
    }
  }

  case class

}

class Variable extends Value {
  // TODO impl
  def toJson: String                         = ""
  override def valueType: ValueType          = ???
  override def toImmutable: Value            = ???
  override def writeTo(packer: Packer): Unit = ???
}
