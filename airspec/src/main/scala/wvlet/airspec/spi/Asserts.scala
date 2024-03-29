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
package wvlet.airspec.spi

import wvlet.airframe.SourceCode
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpecSpi

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.control.NonFatal

object Asserts {
  private[airspec] sealed trait TestResult
  private[airspec] case object Ok     extends TestResult
  private[airspec] case object Failed extends TestResult

  private[airspec] def check(cond: Boolean): TestResult = {
    if (cond) {
      Ok
    } else {
      Failed
    }
  }
}

/**
  */
trait Asserts { this: AirSpecSpi =>
  protected def assert(cond: => Boolean)(implicit code: SourceCode) = {
    if (!cond) {
      throw AssertionFailure("assertion failed", code)
    }
  }

  protected def assert(cond: => Boolean, message: String)(implicit code: SourceCode) = {
    if (!cond) {
      throw AssertionFailure(message, code)
    }
  }

  protected def assertEquals(a: Float, b: Float, delta: Double)(implicit code: SourceCode): Unit = {
    assert((a - b).abs < delta, s"${a} should be ${b} +- ${delta}")(code)
  }

  protected def assertEquals(a: Double, b: Double, delta: Double)(implicit code: SourceCode): Unit = {
    assert((a - b).abs < delta, s"${a} should be ${b} +- ${delta}")(code)
  }

  protected def fail(reason: String = "failed")(implicit code: SourceCode): Unit = {
    throw AssertionFailure(reason, code)
  }

  protected def ignore(reason: String = "ignored")(implicit code: SourceCode): Unit = {
    throw Ignored(reason, code)
  }

  // protected def pending: Unit = macro AirSpecMacros.pendingImpl

  protected def pendingUntil(reason: String = "pending")(implicit code: SourceCode): Unit = {
    throw Pending(reason, code)
  }

  protected def pending(reason: String = "pending")(implicit code: SourceCode): Unit = {
    throw Pending(reason, code)
  }

  protected def cancel(reason: String = "cancelled")(implicit code: SourceCode): Unit = {
    throw Cancelled(reason, code)
  }

  protected def skip(reason: String = "skipped")(implicit code: SourceCode): Unit = {
    throw Skipped(reason, code)
  }

  /**
    * Mark a part of test code as flaky. All failures inside this block will be reported as skipped.
    * @param block
    * @param code
    * @tparam U
    * @return
    */
  protected def flaky[U](block: => U)(implicit code: SourceCode): U = {
    try {
      block match {
        case f: Future[_] =>
          f.recoverWith {
            case e: AirSpecFailureBase =>
              Future.failed(Skipped(s"[flaky] ${e.message}", e.code))
            case NonFatal(e) =>
              Future.failed(Skipped(s"[flaky] ${e.getMessage}", code))
          }(wvlet.airspec.compat.executionContext)
            .asInstanceOf[U]
        case rx: Rx[_] =>
          rx.recoverWith {
            case e: AirSpecFailureBase =>
              Rx.exception(Skipped(s"[flaky] ${e.message}", e.code))
            case NonFatal(e) =>
              Rx.exception(Skipped(s"[flaky] ${e.getMessage}", code))
          }.asInstanceOf[U]
        case other =>
          other
      }
    } catch {
      case e: AirSpecFailureBase =>
        throw Skipped(s"[flaky] ${e.message}", e.code)
      case NonFatal(e) =>
        throw Skipped(s"[flaky] ${e.getMessage}", code)
    }
  }

  protected def intercept[E <: Throwable: ClassTag](block: => Unit)(implicit code: SourceCode): E = {
    val tpe = implicitly[ClassTag[E]]

    try {
      block
      val name = tpe.runtimeClass.getName
      throw InterceptException(s"Expected a ${name} to be thrown", code)
    } catch {
      case ex: InterceptException =>
        throw new AssertionFailure(ex.message, code)
      case ex: Throwable if tpe.runtimeClass.isInstance(ex) =>
        ex.asInstanceOf[E]
    }
  }
}
