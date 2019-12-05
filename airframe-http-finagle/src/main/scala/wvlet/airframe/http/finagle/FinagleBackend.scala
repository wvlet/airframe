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
package wvlet.airframe.http.finagle

import java.util.concurrent.atomic.AtomicReference

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.{Future, Promise, Return, Throw}
import wvlet.airframe.http.{HttpBackend, HttpContext, HttpRequestAdapter}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.{concurrent => sc}

/**
  * Finagle-based implementation of HttpBackend
  */
object FinagleBackend extends HttpBackend[Request, Response, Future] {
  override protected implicit val httpRequestAdapter: HttpRequestAdapter[Request] = FinagleHttpRequestAdapter

  override def wrapException(e: Throwable): Future[Response] = {
    Future.exception(e)
  }
  override def toFuture[A](a: A): Future[A] = Future.value(a)
  override def toScalaFuture[A](a: Future[A]): sc.Future[A] = {
    val promise: sc.Promise[A] = sc.Promise()
    a.respond {
      case Return(value)    => promise.success(value)
      case Throw(exception) => promise.failure(exception)
    }
    promise.future
  }
  override def toFuture[A](a: sc.Future[A], e: ExecutionContext): Future[A] = {
    val promise: Promise[A] = Promise()
    a.onComplete {
      case Success(value)     => promise.setValue(value)
      case Failure(exception) => promise.setException(exception)
    }(e)
    promise
  }

  override def isFutureType(cl: Class[_]): Boolean = {
    classOf[Future[_]].isAssignableFrom(cl)
  }
  override def isRawResponseType(cl: Class[_]): Boolean = {
    classOf[Response].isAssignableFrom(cl)
  }
  override def mapF[A, B](f: Future[A], body: A => B): Future[B] = {
    f.map(body)
  }
  override def newFilter(body: (Request, HttpContext[Request, Response, Future]) => Future[Response]): Filter = {
    new HttpFilterBase {
      override def apply(request: Request, context: HttpContext[Request, Response, Future]): Future[Response] = {
        body(request, context)
      }
    }
  }

  private val contextParamHolderKey = new Contexts.local.Key[AtomicReference[collection.mutable.Map[String, Any]]]

  override def withThreadLocalStore(body: => Future[Response]): Future[Response] = {
    val newParamHolder = collection.mutable.Map.empty[String, Any]
    Contexts.local
      .let(contextParamHolderKey, new AtomicReference[collection.mutable.Map[String, Any]](newParamHolder)) {
        body
      }
  }

  override def setThreadLocal[A](key: String, value: A): Unit = {
    Contexts.local.get(contextParamHolderKey).foreach { ref =>
      ref.get().put(key, value)
    }
  }

  override def getThreadLocal[A](key: String): Option[A] = {
    Contexts.local.get(contextParamHolderKey).flatMap { ref =>
      ref.get.get(key).asInstanceOf[Option[A]]
    }
  }

  override def newContext(body: Request => Future[Response]): HttpContext[Request, Response, Future] =
    new HttpContext[Request, Response, Future] {
      override protected def backend: HttpBackend[Request, Response, Future] = FinagleBackend.this
      override def apply(request: Request): Future[Response] = {
        body(request)
      }
    }

  override def filterAndThenContext(
      filter: FinagleBackend.Filter,
      context: FinagleBackend.Context
  ): FinagleBackend.Context = new Context {
    override protected def backend: HttpBackend[Request, Response, Future] = FinagleBackend.this
    override def apply(request: Request): Future[Response] = {
      rescue {
        filter.apply(request, new SafeHttpContext(context))
      }
    }
  }

  /**
    * Wrapping Context execution with try-catch to return Future[Throwable] upon an error
    */
  private class SafeHttpContext(context: Context) extends Context {
    override def apply(request: Request): Future[Response] = {
      rescue {
        context.apply(request)
      }
    }
    override protected def backend: HttpBackend[Request, Response, Future] = FinagleBackend.this
  }
}
