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
import sbt.testing.{Task, TaskDef}
import wvlet.log.LogSupport

/**
  *
  */
class AirSpecRunner(val args: Array[String], val remoteArgs: Array[String], classLoader: ClassLoader)
    extends sbt.testing.Runner
    with LogSupport {
  override def tasks(taskDefs: Array[TaskDef]): Array[Task] = {
    taskDefs.map { t =>
      info(t)

      new AirSpecTask(t, classLoader)
    }
  }

  override def done(): String = {
    info(s"done")
    ""
  }

  // These methods are defined for Scala.js
  def receiveMessage(msg: String): Option[String] = None
  def deserializeTask(task: String, deserializer: String => sbt.testing.TaskDef): sbt.testing.Task = {
    new AirSpecTask(deserializer(task), classLoader)
  }
  def serializeTask(task: sbt.testing.Task, serializer: sbt.testing.TaskDef => String): String = {
    serializer(task.taskDef())
  }
}

object AirSpecRunner extends LogSupport {
  def newRunner(args: Array[String], remoteArgs: Array[String], testClassLoader: ClassLoader): AirSpecRunner = {
    debug(s"args: ${args.mkString(", ")}")
    debug(s"remote args: ${args.mkString(", ")}")
    new AirSpecRunner(args, remoteArgs, testClassLoader)
  }
}
