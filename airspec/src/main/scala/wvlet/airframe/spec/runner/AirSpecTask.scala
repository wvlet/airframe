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
import sbt.testing.{EventHandler, TaskDef}
import wvlet.airframe.spec.CompatApi
import wvlet.airframe.spec.AirSpecFramework.AirSpecObjectFingerPrint
import wvlet.airframe.spec.spi.AirSpecBase
import wvlet.log.LogSupport

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

/**
  *
  */
class AirSpecTask(override val taskDef: TaskDef, classLoader: ClassLoader) extends sbt.testing.Task with LogSupport {

  override def tags(): Array[String] = Array.empty

  def execute(eventHandler: EventHandler,
              loggers: Array[sbt.testing.Logger],
              continuation: Array[sbt.testing.Task] => Unit): Unit = {
    debug(s"executing task: ${taskDef}")

    val compat: CompatApi = wvlet.airframe.spec.Compat
    compat.withLogScanner {
      try {
        val testClassName = taskDef.fullyQualifiedName()
        val testObj = taskDef.fingerprint() match {
          case AirSpecObjectFingerPrint =>
            compat.findCompanionObjectOf(testClassName, classLoader)
          case _ =>
            compat.newInstanceOf(testClassName, classLoader)
        }

        testObj match {
          case Some(as: AirSpecBase) =>
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

  private def runSpec(spec: AirSpecBase): Unit = {
    spec.getDesign.noLifeCycleLogging.withSession { session =>
      for (m <- spec.testMethods) {
        val args: Seq[Any] = for (p <- m.args) yield {
          session.getInstanceOf(p.surface)
        }
        debug(s"Running ${m.name}(${args.mkString(",")})")
        m.call(spec, args: _*)
      }
    }
  }
}
