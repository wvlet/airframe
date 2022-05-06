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
import wvlet.airframe.{Design, Session}
import wvlet.airspec.runner.AirSpecSbtRunner.AirSpecConfig
import wvlet.airspec.spi.{AirSpecContext, AirSpecException}
import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * AirSpecTaskRunner will execute a task.
  *
  * For each test spec (AirSpec instance), it will create a global airframe session, which can be configured with
  * configure(Design).
  *
  * For each test method in the AirSpec instance, it will create a child session so that users can manage test-method
  * local instances, which will be discarded after the completion of the test method.
  */
private[airspec] class AirSpecTaskRunner(
    taskDef: TaskDef,
    config: AirSpecConfig,
    taskLogger: AirSpecLogger,
    eventHandler: EventHandler,
    classLoader: ClassLoader
) extends LogSupport {
  import wvlet.airspec._

  private implicit val ec: ExecutionContext = Compat.executionContext

  private def findTestInstance(): AirSpecSpi = {
    // Get an instance of AirSpec
    val testObj = taskDef.fingerprint() match {
      // In Scala.js we cannot use pattern match for objects like AirSpecObjectFingerPrint, so using isModule here.
      case c: SubclassFingerprint if c.isModule() =>
        compat.findCompanionObjectOf(testClassName, classLoader)
      case _ =>
        compat.newInstanceOf(testClassName, classLoader)
    }
    testObj match {
      case Some(spec: AirSpecSpi) =>
        spec
      case _ =>
        taskLogger.logSpecName(leafName, indentLevel = 0)
        throw new IllegalStateException(
          s"${testClassName} needs to be a class or object extending AirSpec: ${testObj.getClass}"
        )
    }
  }

  private def testClassName = taskDef.fullyQualifiedName()
  private def leafName      = AirSpecSpi.leafClassName(AirSpecSpi.decodeClassName(testClassName))
  private def specName(parentContext: Option[AirSpecContext], spec: AirSpecSpi): String = {
    val parentName = parentContext.map(x => s"${x.specName}.").getOrElse("")
    s"${parentName}${spec.leafSpecName}"
  }

  /**
    * Process test name filter and extract the target specs
    */
  private def findTargetSpecs(spec: AirSpecSpi): Seq[AirSpecDef] = {
    val testDefs = spec.testDefinitions
    if (testDefs.isEmpty) {
      val name = specName(None, spec)
      warn(s"No test definition is found in ${name}. Add at least one test(...) method call.")
    }

    val selectedSpecs =
      config.pattern match {
        case Some(regex) =>
          // Find matching methods
          testDefs.filter { m =>
            // Concatenate (parent class name)? + class name + method name for handy search
            val fullName = s"${specName(None, spec)}.${m.name}"
            regex.findFirstIn(fullName).isDefined
          }
        case None =>
          testDefs
      }

    selectedSpecs
  }

  private def handleError(e: Throwable, startTimeNanos: Long): Unit = {
    taskLogger.logSpecName(leafName, indentLevel = 0)
    val cause  = compat.findCause(e)
    val status = AirSpecException.classifyException(cause)
    // Unknown error
    val event =
      AirSpecEvent(taskDef, "<spec>", status, new OptionalThrowable(cause), System.nanoTime() - startTimeNanos)
    taskLogger.logEvent(event)
    eventHandler.handle(event)
  }

  def runTask: Future[Unit] = {
    val startTimeNanos = System.nanoTime()
    Future
      .apply {
        // Start a background log level scanner thread. If a thread is already running, reuse it.
        compat.startLogScanner
      }
      .map { _ =>
        val spec = findTestInstance()
        trace(s"[${spec.specName}] Finding a spec instance")
        (spec, findTargetSpecs(spec))
      }
      .flatMap { case (spec: AirSpecSpi, targetSpecs: Seq[AirSpecDef]) =>
        if (targetSpecs.nonEmpty) {
          runSpec(None, spec, targetSpecs)
        } else {
          Future.unit
        }
      }
      .andThen[Unit] { _ =>
        compat.stopLogScanner
      }
      .recover { case e: Throwable =>
        handleError(e, startTimeNanos)
      }
  }

  private def runSpec(
      parentContext: Option[AirSpecContext],
      spec: AirSpecSpi,
      targetTestDefs: Seq[AirSpecDef]
  ): Future[Unit] = {

    def startSpec = Future.apply[Unit] {
      trace(s"[${spec.specName}] Start spec")
      val indentLevel = parentContext.map(_.indentLevel + 1).getOrElse(0)
      taskLogger.logSpecName(spec.leafSpecName, indentLevel = indentLevel)
    }

    def runBeforeAll: Future[Unit] = Future.apply {
      trace(s"[${spec.specName}] beforeAll")
      spec.callBeforeAll
    }

    def runBody: Future[Unit] = Future
      .apply {
        trace(s"[${spec.specName}] Found ${targetTestDefs.size} tests")
        // Configure the global spec design
        var d = Design.newDesign.noLifeCycleLogging
        d = d + spec.callDesign

        // Create a global Airframe session
        val globalSession =
          parentContext
            .map(_.currentSession.newChildSession(d))
            // Do not register JVM shutdown hooks
            .getOrElse(d.newSessionBuilder.noShutdownHook.build)

        trace(s"[${spec.specName}] Start a spec session")
        globalSession.start
        globalSession
      }.flatMap { (globalSession: Session) =>
        val localDesign = spec.callLocalDesign
        val testFuture = targetTestDefs.foldLeft(Future.unit) { (prev, m) =>
          prev.transform { _ =>
            Try(runSingle(parentContext, globalSession, spec, m, isLocal = false, design = localDesign))
          }
        }
        testFuture.andThen { _ =>
          trace(s"[${spec.specName}] Shutdown the spec session")
          globalSession.shutdown
        }
      }

    def runAfterAll: Future[Unit] = Future.apply {
      trace(s"[${spec.specName}] afterAll")
      spec.callAfterAll
    }

    startSpec
      .flatMap(_ => runBeforeAll)
      .flatMap { _ =>
        runBody
      }
      .transformWith {
        case Success(result) =>
          runAfterAll
        case Failure(e) =>
          warn(e)
          runAfterAll.flatMap(_ => Future.failed(e))
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
    trace(s"[${spec.specName}] run test: ${m.name}")

    val ctxName = parentContext.map(ctx => s"${ctx.fullSpecName}.${ctx.testName}").getOrElse("N/A")

    val indentLevel = parentContext.map(_.indentLevel + 1).getOrElse(0)

    // Show the inner test name
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

    var hadChildTask = false

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

          // If the test method had any child task, update the flag
          hadChildTask |= context.hasChildTask
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
    taskLogger.logEvent(e, indentLevel = indentLevel, showTestName = !hadChildTask)
    eventHandler.handle(e)
  }

}
