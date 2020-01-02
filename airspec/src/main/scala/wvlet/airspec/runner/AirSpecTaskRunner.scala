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
import wvlet.airframe.AirframeException.MISSING_DEPENDENCY
import wvlet.airframe.{Design, Session}
import wvlet.airframe.surface.MethodSurface
import wvlet.airspec.AirSpecDef
import wvlet.airspec.runner.AirSpecRunner.AirSpecConfig
import wvlet.airspec.spi.{AirSpecContext, AirSpecException, AirSpecFailureBase, MissingTestDependency}
import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

/**
  * AirSpecTaskRunner will execute a task.
  *
  * For each test spec (AirSpec instance), it will create a global airframe session,
  * which can be configured with configure(Design).
  *
  * For each test method in the AirSpec instance, it will create a child session so that
  * users can manage test-method local instances, which will be discarded after the completion of the test method.
  */
private[airspec] class AirSpecTaskRunner(
    taskDef: TaskDef,
    config: AirSpecConfig,
    taskLogger: AirSpecLogger,
    eventHandler: EventHandler,
    classLoader: ClassLoader
) extends LogSupport {
  import wvlet.airspec._

  def runTask: Unit = {
    val testClassName = taskDef.fullyQualifiedName()
    val leafName      = AirSpecSpi.leafClassName(AirSpecSpi.decodeClassName(testClassName))

    val startTimeNanos = System.nanoTime()
    try {
      // Start a background log level scanner thread. If a thread is already running, reuse it.
      compat.withLogScanner {
        trace(s"Processing task: ${taskDef}")
        // Getting an instance of AirSpec
        val testObj = taskDef.fingerprint() match {
          // In Scala.js we cannot use pattern match for objects like AirSpecObjectFingerPrint, so using isModule here.
          case c: SubclassFingerprint if c.isModule =>
            compat.findCompanionObjectOf(testClassName, classLoader)
          case _ =>
            compat.newInstanceOf(testClassName, classLoader)
        }

        testObj match {
          case Some(spec: AirSpecSpi) =>
            run(parentContext = None, spec, spec.testDefinitions)
          case _ =>
            taskLogger.logSpecName(leafName, indentLevel = 0)
            throw new IllegalStateException(
              s"${testClassName} needs to be a class or object extending AirSpec: ${testObj.getClass}"
            )
        }
      }
    } catch {
      case e: Throwable =>
        taskLogger.logSpecName(leafName, indentLevel = 0)
        val cause  = compat.findCause(e)
        val status = AirSpecException.classifyException(cause)
        // Unknown error
        val event =
          AirSpecEvent(taskDef, "<spec>", status, new OptionalThrowable(cause), System.nanoTime() - startTimeNanos)
        taskLogger.logEvent(event)
        eventHandler.handle(event)
    }
  }

  private[airspec] def run(parentContext: Option[AirSpecContext], spec: AirSpecSpi, testDefs: Seq[AirSpecDef]): Unit = {
    val selectedMethods =
      config.pattern match {
        case Some(regex) =>
          // Find matching methods
          testDefs.filter { m =>
            // Concatenate (parent class name)? + class name + method name for handy search
            val fullName = s"${specName(parentContext, spec)}.${m.name}"
            regex.findFirstIn(fullName).isDefined
          }
        case None =>
          testDefs
      }

    if (selectedMethods.nonEmpty) {
      runSpec(parentContext, spec, selectedMethods)
    }
  }

  private def specName(parentContext: Option[AirSpecContext], spec: AirSpecSpi): String = {
    val parentName = parentContext.map(x => s"${x.specName}.").getOrElse("")
    s"${parentName}${spec.leafSpecName}"
  }

  private def runSpec(
      parentContext: Option[AirSpecContext],
      spec: AirSpecSpi,
      targetTestDefs: Seq[AirSpecDef]
  ): Unit = {
    val indentLevel = parentContext.map(_.indentLevel + 1).getOrElse(0)
    taskLogger.logSpecName(spec.leafSpecName, indentLevel = indentLevel)

    try {
      // Start the spec
      spec.callBeforeAll

      // Configure the global spec design
      var d = Design.newDesign.noLifeCycleLogging
      d = d + spec.callDesign

      // Create a global Airframe session
      val globalSession =
        parentContext
          .map(_.currentSession.newChildSession(d))
          .getOrElse { d.newSessionBuilder.noShutdownHook.build } // Do not register JVM shutdown hooks

      val localDesign = spec.callLocalDesign
      globalSession.start {
        for (m <- targetTestDefs) {
          runSingle(parentContext, globalSession, spec, m, isLocal = false, design = localDesign)
        }
      }
    } finally {
      spec.callAfterAll
    }
  }

  private var displayedContext = Set.empty[String]

  private[airspec] def runSingle(
      parentContext: Option[AirSpecContext],
      globalSession: Session,
      spec: AirSpecSpi,
      m: AirSpecDef,
      isLocal: Boolean,
      design: Design
  ): Unit = {

    val ctxName = parentContext.map(ctx => s"${ctx.fullSpecName}.${ctx.testName}").getOrElse("N/A")

    val indentLevel = parentContext.map(_.indentLevel + 1).getOrElse(0)
    if (isLocal) {
      parentContext.map { ctx =>
        synchronized {
          if (!displayedContext.contains(ctxName)) {
            taskLogger.logTestName(ctx.testName, indentLevel = (indentLevel - 1).max(0))
            displayedContext += ctxName
          }
        }
      }
    }

    spec.callBefore
    // Configure the test-local design
    val childDesign = design + m.design

    val startTimeNanos = System.nanoTime()
    // Create a test-method local child session
    val result = globalSession.withChildSession(childDesign) { childSession =>
      val context =
        new AirSpecContextImpl(
          this,
          parentContext = parentContext,
          currentSpec = spec,
          testName = m.name,
          currentSession = childSession
        )
      spec.pushContext(context)
      // Wrap the execution with Try[_] to report the test result to the event handler
      Try {
        try {
          m.run(context, childSession)
        } finally {
          spec.callAfter
          spec.popContext
        }
      }
    }
    // Report the test result
    val durationNanos = System.nanoTime() - startTimeNanos

    val (status, throwableOpt) = result match {
      case Success(x) =>
        (Status.Success, new OptionalThrowable())
      case Failure(ex) =>
        val status = AirSpecException.classifyException(ex)
        (status, new OptionalThrowable(compat.findCause(ex)))
    }

    val e = AirSpecEvent(taskDef, m.name, status, throwableOpt, durationNanos)
    taskLogger.logEvent(e, indentLevel = indentLevel)
    eventHandler.handle(e)
  }

}
