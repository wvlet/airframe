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
package wvlet.airframe.jmx

import wvlet.airframe.surface.{ParameterBase, Surface}

sealed trait MBeanParameter {
  def name: String
  def description: String
  def get(obj: AnyRef): AnyRef
  def valueType: Surface
}

case class MBeanObjectParameter(name: String, description: String, param: ParameterBase) extends MBeanParameter {
  def valueType = param.surface
  override def get(obj: AnyRef): AnyRef = {
    param.call(obj).asInstanceOf[AnyRef]
  }
}

case class NestedMBeanParameter(
    name: String,
    description: String,
    parentParam: ParameterBase,
    nestedParam: ParameterBase
) extends MBeanParameter {
  def valueType = nestedParam.surface
  override def get(obj: AnyRef): AnyRef = {
    nestedParam.call(parentParam.call(obj)).asInstanceOf[AnyRef]
  }
}
