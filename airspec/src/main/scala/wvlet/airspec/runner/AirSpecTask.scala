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
package wvlet.airspec.runner

import sbt.testing._
import wvlet.airframe.Design
import wvlet.airspec.runner.AirSpecSbtRunner.AirSpecConfig
import wvlet.log.LogSupport

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

/**
  * AirSpecTask is a unit of test execution.
  */
private[airspec] class AirSpecTask(
    config: AirSpecConfig,
    taskLogger: AirSpecLogger,
    override val taskDef: TaskDef,
    classLoader: ClassLoader
) extends sbt.testing.Task
    with LogSupport {
  override def tags(): Array[String] = Array.empty

  /**
    * This method will be used only for Scala (JVM). This will delegate the task execution process to execute(handler,
    * logger, continuation)
    */
  def execute(eventHandler: EventHandler, loggers: Array[sbt.testing.Logger]): Array[sbt.testing.Task] = {
    val p = Promise[Unit]()
    execute(eventHandler, loggers, _ => p.success(()))
    Await.result(p.future, Duration.Inf)
    Array.empty
  }

  /**
    * Scala.js specific executor:
    * [[[sbt.testing.Task.execute(eventHandler:sbt\.testing\.EventHandler,loggers:Array[sbt\.testing\.Logger])* execute]]
    * ] but takes a continuation.
    *
    * This is to support JavaScripts asynchronous nature.
    *
    * When running in a JavaScript environment, only this method will be called.
    */
  def execute(
      eventHandler: EventHandler,
      loggers: Array[sbt.testing.Logger],
      continuation: Array[sbt.testing.Task] => Unit
  ): Unit = {
    try {
      new AirSpecTaskRunner(taskDef, config, taskLogger, eventHandler, classLoader).runTask
    } finally {
      continuation(Array.empty)
    }
  }
}
