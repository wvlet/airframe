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

import scala.reflect.ClassTag

package object parallel {
  implicit class ToParallelSeq[T](source: Seq[T]) {
    def parallel: ParallelSeq[T] = ParallelSeq(source)
  }

  implicit class ToParallelIterator[T](source: Iterator[T]) {
    def parallel: ParallelIterator[T] = ParallelIterator(source)
  }

  case class ParallelSeq[T](
      private val source: Seq[T],
      private val parallelism: Int = Runtime.getRuntime.availableProcessors()
  ) {
    def withParallelism(parallelism: Int): ParallelSeq[T] = {
      copy(parallelism = parallelism)
    }

    def map[R: ClassTag](f: T => R): Seq[R] = {
      Parallel.run(source, parallelism = parallelism)(f)
    }
  }

  case class ParallelIterator[T](
      private val source: Iterator[T],
      private val parallelism: Int = Runtime.getRuntime.availableProcessors()
  ) {
    def withParallelism(parallelism: Int): ParallelIterator[T] = {
      copy(parallelism = parallelism)
    }

    def map[R: ClassTag](f: T => R): Iterator[R] = {
      Parallel.iterate(source, parallelism = parallelism)(f)
    }
  }
}
