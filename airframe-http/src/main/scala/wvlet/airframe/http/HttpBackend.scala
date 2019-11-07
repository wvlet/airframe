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
package wvlet.airframe.http

import wvlet.airframe.http.HttpFilter.HttpFilterFactory

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

/**
  * A base interface to implement http-server specific implementation
  */
trait HttpBackend[Req, Resp, F[_]] extends HttpFilterFactory[Req, Resp, F] {
  def toFuture[A](a: A): F[A]
  // Convert Scala's Future into the this backend's Future
  def toFuture[A](a: scala.concurrent.Future[A], e: ExecutionContext): F[A]
  def toScalaFuture[A](a: F[A]): scala.concurrent.Future[A]
  def wrapException(e: Throwable): F[Resp]
  def isFutureType(x: Class[_]): Boolean
  def isScalaFutureType(x: Class[_]): Boolean = {
    classOf[scala.concurrent.Future[_]].isAssignableFrom(x)
  }
  def isRawResponseType(x: Class[_]): Boolean

  def mapF[A, B](f: F[A], body: A => B): F[B]

  def newFilter(body: (Req, HttpContext[Req, Resp, F]) => F[Resp]): Filter
}
