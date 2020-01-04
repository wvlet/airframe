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

trait TaskContext[In, Out] {
  def apply(in: In): Future[Out]
}

import Task._

trait Task[In, Out] {
  def apply(input: In, context: TaskContext[In, Out]): Future[Out]
  def andThen(nextTask: Task[In, Out]): Task[In, Out]              = new AndThen(this, nextTask)
  def andThen(context: TaskContext[In, Out]): TaskContext[In, Out] = new AndThenContext(this, context)
}

/**
  *
  */
object Task {

  def newTask[In, Out](body: (In, TaskContext[In, Out]) => Future[Out]): Task[In, Out] = new Task[In, Out] {
    override def apply(
        input: In,
        context: TaskContext[In, Out]
    ): Future[Out] = {
      try {
        body(input, context)
      } catch {
        case NonFatal(e) => Future.failed[Out](e)
      }
    }
  }

  private class AndThen[In, Out](prev: Task[In, Out], next: Task[In, Out]) extends Task[In, Out] {
    def apply(input: In, context: TaskContext[In, Out]): Future[Out] = {
      try {
        prev.apply(input, next.andThen(context))
      } catch {
        case NonFatal(e) => Future.failed[Out](e)
      }
    }
  }

  private class AndThenContext[In, Out](task: Task[In, Out], context: TaskContext[In, Out])
      extends TaskContext[In, Out] {
    override def apply(in: In): Future[Out] = {
      try {
        task.apply(in, context)
      } catch {
        case NonFatal(e) => Future.failed[Out](e)
      }
    }
  }
}
