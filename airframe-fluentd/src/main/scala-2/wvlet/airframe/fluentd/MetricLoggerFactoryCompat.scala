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
package wvlet.airframe.fluentd
import wvlet.airframe.surface.Surface

import scala.reflect.runtime.{universe => ru}

trait MetricLoggerFactoryCompat {
  self: MetricLoggerFactory =>

  def getTypedLogger[T <: TaggedMetric: ru.TypeTag]: TypedMetricLogger[T] = {
    self.getTypedLoggerInternal[T](Surface.of[T], None)
  }

  def getTypedLoggerWithTagPrefix[T <: TaggedMetric: ru.TypeTag](tagPrefix: String): TypedMetricLogger[T] = {
    self.getTypedLoggerInternal[T](Surface.of[T], Some(tagPrefix))
  }
}
