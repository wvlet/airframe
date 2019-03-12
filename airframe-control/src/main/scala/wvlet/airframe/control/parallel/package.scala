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

  implicit class ParallelSeq[T](source: Seq[T]) {
    def parallelMap[R: ClassTag](parallelism: Int = Runtime.getRuntime.availableProcessors())(f: T => R): Seq[R] = {
      Parallel.run(source, parallelism)(f)
    }
  }

  implicit class ParallelIterator[T](source: Iterator[T]) {
    def parallelMap[R: ClassTag](parallelism: Int = Runtime.getRuntime.availableProcessors())(
        f: T => R): Iterator[R] = {
      Parallel.iterate(source, parallelism)(f)
    }
  }

}
