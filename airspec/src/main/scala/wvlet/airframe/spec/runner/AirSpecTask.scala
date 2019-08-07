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
package wvlet.airframe.spec.runner
import sbt.testing.{EventHandler, Logger, Task, TaskDef}
import wvlet.log.LogSupport

/**
  *
  */
class AirSpecTask(inputTaskDef: TaskDef, classLoader: ClassLoader) extends sbt.testing.Task with LogSupport {
  override def tags(): Array[String] = Array.empty
  override def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[Task] = {
    info(s"executing task: ${taskDef}")

    Array.empty
  }

  override def taskDef: TaskDef = inputTaskDef
}
