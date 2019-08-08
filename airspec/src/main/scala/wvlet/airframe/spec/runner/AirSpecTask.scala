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
import java.util.concurrent.TimeUnit

import sbt.testing.{Event, EventHandler, Fingerprint, Logger, OptionalThrowable, Selector, Status, Task, TaskDef}
import wvlet.airframe.Session
import wvlet.airframe.spec.AirSpecFramework.AirSpecObjectFingerPrint
import wvlet.airframe.spec.spi.{AirSpecBase, AirSpecException, AssertionFailure}
import wvlet.log.LogSupport
import wvlet.airframe.spec._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.{Failure, Success, Try}

/**
  *
  */
class AirSpecTask(override val taskDef: TaskDef, classLoader: ClassLoader) extends sbt.testing.Task with LogSupport {

  override def tags(): Array[String] = Array.empty

  def execute(eventHandler: EventHandler, loggers: Array[sbt.testing.Logger]): Array[sbt.testing.Task] = {
    val p = Promise[Unit]()
    execute(eventHandler, loggers, _ => p.success(()))
    Await.result(p.future, Duration.Inf)
    Array.empty
  }

  /**
    * Scala.js specific: Same as basic
    * [[[sbt.testing.Task.execute(eventHandler:sbt\.testing\.EventHandler,loggers:Array[sbt\.testing\.Logger])*
    * execute]]]
    * but takes a continuation.
    *
    * This is to support JavaScripts asynchronous nature.
    *
    * When running in a JavaScript environment, only this method will be
    * called.
    */
  def execute(eventHandler: EventHandler,
              loggers: Array[sbt.testing.Logger],
              continuation: Array[sbt.testing.Task] => Unit): Unit = {
    debug(s"executing task: ${taskDef}")

    def log(msg: String): Unit = {
      info(msg)
    }

    def runSpec(spec: AirSpecBase): Unit = {
      // TODO sanitize name
      log(s"[${spec.getClass.getSimpleName}]")
      spec.getDesign.noLifeCycleLogging.withSession { session =>
        for (m <- spec.testMethods) {
          val args: Seq[Any] = for (p <- m.args) yield {
            session.getInstanceOf(p.surface)
          }
          val argStr = if (args.isEmpty) "" else s"(${args.mkString(",")})"
          log(s"- ${m.name}${argStr}")

          val startTimeNanos = System.nanoTime()
          val result         = Try(m.call(spec, args: _*))
          val durationNanos  = System.nanoTime() - startTimeNanos
          reportEvent(eventHandler: EventHandler, result, durationNanos)
        }
      }
    }

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
            runSpec(as)
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

  import AirSpecTask._
  private def reportEvent(eventHandler: EventHandler, result: Try[_], durationNanos: Long): Unit = {
    val (status, throwableOpt) = result match {
      case Success(x) =>
        (Status.Success, new OptionalThrowable())
      case Failure(ex) =>
        (AirSpecException.classifyException(ex), new OptionalThrowable(ex))
    }
    try {
      val e = AirSpecEvent(taskDef, status, throwableOpt, durationNanos)
      eventHandler.handle(e)
    } catch {
      case e: Throwable =>
        warn(e)
    }
  }

}

object AirSpecTask {

  private[spec] case class AirSpecEvent(taskDef: TaskDef,
                                        override val status: Status,
                                        override val throwable: OptionalThrowable,
                                        durationNanos: Long)
      extends Event {
    override def fullyQualifiedName(): String = taskDef.fullyQualifiedName()
    override def fingerprint(): Fingerprint   = taskDef.fingerprint()
    override def selector(): Selector         = taskDef.selectors().head
    override def duration(): Long             = TimeUnit.NANOSECONDS.toMillis(durationNanos)
  }

}

class MethodTask(session: Session, testInstnace: AirSpecBase, override val taskDef: TaskDef, classLoader: ClassLoader)
    extends sbt.testing.Task
    with LogSupport {
  override def tags(): Array[String] = Array.empty
  override def execute(eventHandler: EventHandler, loggers: Array[Logger]): Array[Task] = {
    Array.empty
  }
}
