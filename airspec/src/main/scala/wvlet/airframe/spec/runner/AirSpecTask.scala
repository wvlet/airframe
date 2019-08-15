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
import java.util.regex.Pattern

import sbt.testing._
import wvlet.airframe.AirframeException.MISSING_DEPENDENCY
import wvlet.airframe.Design
import wvlet.airframe.spec._
import wvlet.airframe.spec.runner.AirSpecRunner.AirSpecConfig
import wvlet.airframe.spec.spi.{AirSpecContext, AirSpecException, MissingTestDependency}
import wvlet.airframe.surface.MethodSurface
import wvlet.log.LogSupport

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.{Failure, Success, Try}

/**
  * AirSpecTask is a unit of test execution.
  *
  * For each test spec (AirSpec instance), it will create a global airframe session,
  * which can be configured with configure(Design).
  *
  * For each test method in the AirSpec instance, it will create a child session so that
  * users can manage test-method local instances, which will be discarded after the completion of the test method.
  */
private[spec] class AirSpecTask(config: AirSpecConfig, override val taskDef: TaskDef, classLoader: ClassLoader)
    extends sbt.testing.Task
    with LogSupport {

  import AirSpecTask._

  override def tags(): Array[String] = Array.empty

  /**
    * This method will be used only for Scala (JVM). This will delegate the task execution process to
    * execute(handler, logger, continuation)
    */
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

    val taskLogger = new AirSpecLogger(loggers)

    import AirSpecSpi._

    val startTimeNanos = System.nanoTime()
    try {
      compat.withLogScanner {
        trace(s"Executing task: ${taskDef}")
        val testObj = taskDef.fingerprint() match {
          // In Scala.js we cannot use pattern match for objects like AirSpecObjectFingerPrint
          case c: SubclassFingerprint if c.isModule =>
            compat.findCompanionObjectOf(testClassName, classLoader)
          case _ =>
            compat.newInstanceOf(testClassName, classLoader)
        }

        testObj match {
          case Some(spec: AirSpec) =>
            new TaskExecutor(taskDef, config, taskLogger, eventHandler)
              .run(spec, spec.testMethods)
          case _ =>
            taskLogger.logSpecName(decodeClassName(taskDef.fullyQualifiedName()))
            throw new IllegalStateException(s"${testClassName} needs to be a class (or an object) extending AirSpec")
        }
      }
    } catch {
      case e: Throwable =>
        // Unknown error
        val event =
          AirSpecEvent(taskDef, "<spec>", Status.Error, new OptionalThrowable(e), System.nanoTime() - startTimeNanos)
        taskLogger.logEvent(event)
        eventHandler.handle(event)
    } finally {
      continuation(Array.empty)
    }
  }
}

object AirSpecTask {

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

  private[spec] class TaskExecutor(taskDef: TaskDef,
                                   config: AirSpecConfig,
                                   taskLogger: AirSpecLogger,
                                   eventHandler: EventHandler) {

    def run(spec: AirSpecSpi, testMethods: Seq[MethodSurface]): Unit = {
      val selectedMethods =
        config.pattern match {
          case Some(regex) =>
            // Find matching methods
            testMethods.filter { m =>
              // Concatenate class name + method name for handy search
              val fullName = s"${spec.specName}:${m.name}"
              regex.findFirstIn(fullName).isDefined
            }
          case None =>
            testMethods
        }

      if (selectedMethods.nonEmpty) {
        runSpec(spec, selectedMethods)
      }
    }

    private def runSpec(spec: AirSpecSpi, targetMethods: Seq[MethodSurface]): Unit = {
      taskLogger.logSpecName(spec.leafSpecName)

      try {
        // Start the spec
        spec.callBeforeAll

        var d = Design.newDesign.noLifeCycleLogging
        // Allow configuring the global spec design
        d = spec.callDesignAll(d)

        // Create a new Airframe session
        d.withSession { session =>
          for (m <- targetMethods) {
            spec.callBefore
            // Allow configuring the test-local design
            val childDesign = spec.callDesignEach(Design.newDesign)

            val startTimeNanos = System.nanoTime()
            // Create a test-method local child session
            val result = session.withChildSession(childDesign) { childSession =>
              val context =
                new AirSpecContextImpl(this,
                                       specName = spec.leafSpecName,
                                       testName = m.name,
                                       currentSession = childSession)
              Try {
                try {
                  // Build a list of method arguments
                  val args: Seq[Any] = for (p <- m.args) yield {
                    try {
                      p.surface.rawType match {
                        case cls if classOf[AirSpecContext].isAssignableFrom(cls) =>
                          context
                        case _ =>
                          childSession.getInstanceOf(p.surface)
                      }
                    } catch {
                      case e @ MISSING_DEPENDENCY(stack, _) =>
                        throw MissingTestDependency(
                          s"Failed to call ${spec.leafSpecName}.`${m.name}`. Missing dependency for ${p.name}:${p.surface}")
                    }
                  }
                  // Call the test method
                  m.call(spec, args: _*)
                } finally {
                  spec.callAfter
                }
              }
            }
            val durationNanos = System.nanoTime() - startTimeNanos

            val (status, throwableOpt) = result match {
              case Success(x) =>
                (Status.Success, new OptionalThrowable())
              case Failure(ex) =>
                val status = AirSpecException.classifyException(ex)
                (status, new OptionalThrowable(compat.findCause(ex)))
            }

            val e = AirSpecEvent(taskDef, m.name, status, throwableOpt, durationNanos)
            taskLogger.logEvent(e)
            eventHandler.handle(e)
          }
        }
      } finally {
        spec.callAfterAll
      }
    }
  }

}
