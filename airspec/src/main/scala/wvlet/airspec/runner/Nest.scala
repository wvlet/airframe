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
import scala.concurrent.Future
import scala.util.control.NonFatal

trait Context[Out] {
  def eval: Task[Out]
  def add[A](a:A): Context
  def get[A]: Option[A]
}

trait Task[A] {
  def map[B](f: A=>B): Task[B]
  def rescue[U](pf: PartialFunction[Throwable, U]): Task[U]
}

object Task {
  def fromFuture[A](f: scala.concurrent.Future[A]): Task[A] = {
    //...
  }
}

trait Nest[A] {
  def apply(context: Context[A]): Task[A]
  def andThen(next: Nest[A]): Nest[A]
}

import Nest._

trait Nest[In, Out] {
  def apply(input: In, context: NestContext[In, Out]): Future[Out]
  def andThen(nextNest : Nest[In, Out]): Nest[In, Out]              = new AndThen(this, nextNest)
  def andThen(context: NestContext[In, Out]): Nest Context[In, Out] = new AndThenContext(this, context)
}

/**
  *
  */
object Nest {

  def newNest[In, Out](body: (In, NestContext[In, Out]) => Future[Out]): Nest[In, Out] = new Nest[In, Out] {
    override def apply(
        input: In,
        context: NestContext[In, Out]
    ): Future[Out] = {
      try {
        body(input, context)
      } catch {
        case NonFatal(e) => Future.failed[Out](e)
      }
    }
  }

  private class AndThen[In, Out](prev: Nest[In, Out], next: Nest[In, Out]) extends Nest[In, Out] {
    def apply(input: In, context: NestContext[In, Out]): Future[Out] = {
      try {
        prev.apply(input, next.andThen(context))
      } catch {
        case NonFatal(e) => Future.failed[Out](e)
      }
    }
  }

  private class AndThenContext[In, Out](nest: Nest[In, Out], context: NestContext[In, Out])
      extends NestContext[In, Out] {
    override def apply(in: In): Future[Out] = {
      try {
        nest.apply(in, context)
      } catch {
        case NonFatal(e) => Future.failed[Out](e)
      }
    }
  }
}
