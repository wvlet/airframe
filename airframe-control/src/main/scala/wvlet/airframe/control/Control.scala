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
package wvlet.airframe.control
import scala.util.control.NonFatal
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  */
object Control {
  def withResource[R <: AutoCloseable, U](resource: R)(body: R => U): U = {
    try {
      body(resource)
    } finally {
      if (resource != null) {
        resource.close()
      }
    }
  }

  def withResources[R1 <: AutoCloseable, R2 <: AutoCloseable, U](resource1: R1, resource2: R2)(
      body: (R1, R2) => U
  ): U = {
    try {
      body(resource1, resource2)
    } finally {
      closeResources(resource1, resource2)
    }
  }

  /**
    * A loan pattern for Future[U].
    *
    * TODO: Test this after async test is available in 22.5.0
    */
  def withResourceAsync[R <: AutoCloseable, U](
      resource: R
  )(body: R => Future[U])(implicit sc: ExecutionContext): Future[U] = {
    Future
      .apply(resource)
      .flatMap(r => body(r))
      .transform { case any =>
        closeSafely(any, () => resource.close())
      }
  }

  /**
    * Safely close the resource. If the resource is closed successfully, return the preceding result, if closing the
    * resource is failed, combine the previous exception (if exists) and the resource closing exception together.
    */
  private[control] def closeSafely[U](preceding: Try[U], close: () => Unit): Try[U] = {
    Try(close()) match {
      case Success(_) =>
        preceding
      case Failure(e) =>
        // If closing the resource failed, report the error in the Future
        preceding match {
          case Success(x) =>
            Failure(e)
          case Failure(ex) =>
            Failure(MultipleExceptions(List(e, ex)))
        }
    }
  }

  def closeResources[R <: AutoCloseable](resources: R*): Unit = {
    if (resources != null) {
      var exceptionList = List.empty[Throwable]
      resources.map { x =>
        try {
          if (x != null) {
            x.close()
          }
        } catch {
          case NonFatal(e) =>
            exceptionList = e :: exceptionList
        }
      }
      if (exceptionList.nonEmpty) {
        throw MultipleExceptions(exceptionList)
      }
    }
  }
}
