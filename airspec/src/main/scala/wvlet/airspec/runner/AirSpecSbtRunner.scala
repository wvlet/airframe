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

import sbt.testing.{Task, TaskDef}
import wvlet.airspec.runner.AirSpecSbtRunner.AirSpecConfig
import wvlet.log.{LogSupport, Logger, LogLevel}

import scala.util.matching.Regex

/**
  * d AirSpecRunner receives a list of TaskDefs from sbt, then create AirSpecTasks to execute.
  */
private[airspec] class AirSpecSbtRunner(config: AirSpecConfig, _remoteArgs: Array[String], classLoader: ClassLoader)
    extends sbt.testing.Runner {

  private lazy val taskLogger = new AirSpecLogger()

  override def args: Array[String]         = config.args
  override def remoteArgs(): Array[String] = _remoteArgs

  override def tasks(taskDefs: Array[TaskDef]): Array[Task] = {
    taskDefs
      .map { t =>
        new AirSpecTask(config, taskLogger, t, classLoader)
      }
  }

  override def done(): String = {
    // sbt 1.3.x's layered class loader will not clean up LogHandlers
    // registered at java.util.logging, so we need to unregister all LogHandlers implementations that
    // use airframe's code before sbt detaches the class loader
    taskLogger.clearHandlers
    Logger.clearAllHandlers
    Logger.setDefaultLogLevel(LogLevel.INFO)
    ""
  }

  // The following methods are defined for Scala.js support:
  def receiveMessage(msg: String): Option[String] = None
  def deserializeTask(task: String, deserializer: String => sbt.testing.TaskDef): sbt.testing.Task = {
    new AirSpecTask(config, taskLogger, deserializer(task), classLoader)
  }
  def serializeTask(task: sbt.testing.Task, serializer: sbt.testing.TaskDef => String): String = {
    serializer(task.taskDef())
  }
}

private[airspec] object AirSpecSbtRunner extends LogSupport {
  def newRunner(args: Array[String], remoteArgs: Array[String], testClassLoader: ClassLoader): AirSpecSbtRunner = {
    new AirSpecSbtRunner(
      AirSpecConfig(args),
      remoteArgs,
      testClassLoader
    )
  }

  case class AirSpecConfig(
      args: Array[String]
  ) extends LogSupport {
    private var remainingArgs: Array[String] = _
    private var _defaultLogLevel: LogLevel   = LogLevel.INFO
    private var _additionalLogLevels         = Map.empty[String, LogLevel]

    init

    private def init: Unit = {
      // Set log level with -l (log level)
      val remaining           = Array.newBuilder[String]
      var i                   = 0
      var logLevel: LogLevel  = Logger.getDefaultLogLevel
      val additionalLogLevels = Map.newBuilder[String, LogLevel]
      while i < args.length do {
        args(i) match {
          case "-l" if i < args.length - 1 =>
            // Set the default log level for the test spec
            logLevel = LogLevel(args(i + 1))
            i += 1
          case arg if arg.startsWith("-L") =>
            arg.stripPrefix("-L").split("=") match {
              case Array(pkg, level) =>
                additionalLogLevels += pkg -> LogLevel(level)
              case _ =>
                warn(s"Ignoring invalid argument: ${arg}. Use -L(package)=(log level) to set log levels")
            }
          case other =>
            remaining += other
        }
        i += 1
      }

      remainingArgs = remaining.result()
      _defaultLogLevel = logLevel
      _additionalLogLevels = additionalLogLevels.result()
    }

    def defaultLogLevel: LogLevel                  = _defaultLogLevel
    def additionalLogLevels: Map[String, LogLevel] = _additionalLogLevels

    val specMatcher: AirSpecMatcher = {
      // For now, we only support regex-based test name matcher using the first argument
      remainingArgs.find(x => !x.startsWith("-")) match {
        case Some(p) => new AirSpecMatcher(p)
        case None    => AirSpecMatcher.all
      }
    }
  }
}
