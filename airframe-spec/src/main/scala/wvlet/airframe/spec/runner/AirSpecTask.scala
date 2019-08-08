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
import java.util.Locale
import java.util.concurrent.TimeUnit

import sbt.testing._
import wvlet.airframe.spec.AirSpecFramework.AirSpecObjectFingerPrint
import wvlet.airframe.spec._
import wvlet.airframe.spec.spi.AirSpecException
import wvlet.log.{LogSupport, Logger}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.{Failure, Success, Try}

/**
  *
  */
class AirSpecTask(override val taskDef: TaskDef, classLoader: ClassLoader) extends sbt.testing.Task with LogSupport {

  import AirSpecTask._

  private val taskLogger = Logger("wvlet.airframe.spec.runner.TaskLogger")
  taskLogger.setFormatter(AirSpecLogFormatter)

  override def tags(): Array[String] = Array.empty

  def execute(eventHandler: EventHandler, loggers: Array[sbt.testing.Logger]): Array[sbt.testing.Task] = {
    val p = Promise[Unit]()
    execute(eventHandler, loggers, _ => p.success(()))
    Await.result(p.future, Duration.Inf)
    Array.empty
  }

  /**
    * Scala.js specific executor:
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

    val testClassName = taskDef.fullyQualifiedName()

    def runSpec(spec: AirSpecSpi): Unit = {
      val clsLeafName = decodeClassName(spec.getClass)
      taskLogger.info(s"${clsLeafName}:")
      spec.getDesign.noLifeCycleLogging.withSession { session =>
        for (m <- spec.testMethods) {
          val args: Seq[Any] = for (p <- m.args) yield {
            session.getInstanceOf(p.surface)
          }
          val argStr = if (args.isEmpty) "" else s"(${args.mkString(",")})"

          val startTimeNanos = System.nanoTime()
          val result = Try {
            m.call(spec, args: _*)
          }
          val durationNanos = System.nanoTime() - startTimeNanos
          reportEvent(eventHandler: EventHandler, clsName = testClassName, testName = m.name, result, durationNanos)
        }
      }
    }

    try {
      compat.withLogScanner {
        trace(s"Executing a task: ${taskDef}")
        val testObj = taskDef.fingerprint() match {
          case AirSpecObjectFingerPrint =>
            compat.findCompanionObjectOf(testClassName, classLoader)
          case _ =>
            compat.newInstanceOf(testClassName, classLoader)
        }

        testObj match {
          case Some(as: AirSpecSpi) =>
            runSpec(as)
          case other =>
            taskLogger.warn(s"Failed to instantiate: ${testClassName}")
        }
      }
    } catch {
      case e: Throwable =>
        // Unknown error
        reportError(taskDef.fullyQualifiedName(), e, Status.Error)
    } finally {
      continuation(Array.empty)
    }
  }

  import AirSpecTask._
  private def reportEvent(eventHandler: EventHandler,
                          clsName: String,
                          testName: String,
                          result: Try[_],
                          durationNanos: Long): Unit = {
    val (status, throwableOpt) = result match {
      case Success(x) =>
        taskLogger.info(s" - ${testName}")
        (Status.Success, new OptionalThrowable())
      case Failure(ex) =>
        val status = AirSpecException.classifyException(ex)
        reportError(testName: String, ex, status)
        (status, new OptionalThrowable(compat.findCause(ex)))
    }
    try {
      val e = AirSpecEvent(taskDef, testName, status, throwableOpt, durationNanos)
      eventHandler.handle(e)
    } catch {
      case e: Throwable =>
        taskLogger.warn(e)
    }
  }

  private def formatError(testName: String, status: Status, e: AirSpecException): String = {
    s" - ${testName} -- ${status.toString.toLowerCase(Locale.ENGLISH)}  (${e.code})"
  }

  private def reportError(testName: String, e: Throwable, status: Status): Unit = {
    val cause = compat.findCause(e)
    cause match {
      case e: AirSpecException =>
        val msg = formatError(testName, status, e)
        status match {
          case Status.Failure =>
            taskLogger.error(msg)
          case Status.Error =>
            taskLogger.error(msg)
          case _ =>
            taskLogger.warn(msg)
        }
      case other =>
        taskLogger.error(s" - ${testName} -- Error ${other.getMessage}", e)
    }
  }

}

object AirSpecTask {

  private[spec] def decodeClassName(cls: Class[_]): String = {
    // Scala.js doesn't produce a clean class name with cls.getSimpleName(), so we need to use
    // the full class name
    val decodedClassName = scala.reflect.NameTransformer.decode(cls.getName)
    val pos              = decodedClassName.lastIndexOf('.')
    var name =
      if (pos == -1)
        decodedClassName
      else
        decodedClassName.substring((pos + 1).min(decodedClassName.length - 1))

    // For object names ending with $
    if (name.endsWith("$")) {
      name = name.substring(0, name.length - 1)
    }
    name
  }

  private[spec] case class AirSpecEvent(taskDef: TaskDef,
                                        override val fullyQualifiedName: String,
                                        override val status: Status,
                                        override val throwable: OptionalThrowable,
                                        durationNanos: Long)
      extends Event {
    override def fingerprint(): Fingerprint = taskDef.fingerprint()
    override def selector(): Selector       = taskDef.selectors().head
    override def duration(): Long           = TimeUnit.NANOSECONDS.toMillis(durationNanos)
  }

}
