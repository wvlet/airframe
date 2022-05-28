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

import scala.reflect.runtime.{universe => ru}
import wvlet.airframe.surface.Surface
import scala.language.experimental.macros

trait JMXMBeanCompat {
  def of[A: ru.WeakTypeTag](obj: A): JMXMBean = {
    JMXMBean.of(obj, Surface.of[A], Surface.methodsOf[A])
  }

}
trait JMXRegistryCompat { self: JMXRegistry =>
  def register[A: ru.WeakTypeTag](obj: A): Unit = {
    val mbean = JMXMBean.of[A](obj)
    self.register(mbean, obj)
  }
}
