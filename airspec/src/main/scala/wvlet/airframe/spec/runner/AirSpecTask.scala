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
import wvlet.airframe.spec.Framework.AirSpecObjectFingerPrint
import wvlet.airframe.spec.spi.AirSpec
import wvlet.airframe.spec.Compat
import wvlet.log.{LogSupport, Logger}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration

/**
  *
  */
class AirSpecTask(override val taskDef: TaskDef, classLoader: ClassLoader) extends sbt.testing.Task with LogSupport {

  override def tags(): Array[String] = Array.empty

  def execute(eventHandler: EventHandler,
              loggers: Array[sbt.testing.Logger],
              continuation: Array[sbt.testing.Task] => Unit): Unit = {
    info(s"executing task: ${taskDef}")

    Compat.withLogScanner {
      try {
        val testClassName = taskDef.fullyQualifiedName()
        val testObj = taskDef.fingerprint() match {
          case AirSpecObjectFingerPrint =>
            Compat.findCompanionObjectOf(testClassName, classLoader)
          case _ =>
            Compat.newInstanceOf(testClassName, classLoader)
        }

        testObj match {
          case Some(as: AirSpec) =>
            AirSpecTask.runSpec(as)
          case other =>
            warn(s"Failed to instantiate: ${testClassName}")
        }

        continuation(Array.empty)
      } catch {
        case e: Throwable =>
          warn(e.getMessage)
      }
    }
  }

  def execute(eventHandler: EventHandler, loggers: Array[sbt.testing.Logger]): Array[sbt.testing.Task] = {
    val p = Promise[Unit]()
    execute(eventHandler, loggers, _ => p.success(()))
    Await.result(p.future, Duration.Inf)
    Array.empty
  }

}

object AirSpecTask extends LogSupport {

  private def runSpec(spec: AirSpec): Unit = {
    spec.getDesign.noLifeCycleLogging.withSession { session =>
      for (m <- spec.testMethods) {
        val args: Seq[Any] = for (p <- m.args) yield {
          session.getInstanceOf(p.surface)
        }
        info(s"Running ${m.name}(${args.mkString(",")})")
        m.call(spec, args: _*)
      }
    }
  }
}
