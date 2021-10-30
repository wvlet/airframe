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

package wvlet.airframe.surface

import java.time.temporal.ChronoUnit
import java.io.File

object SurfaceJVMTest {
  type MyChrono = ChronoUnit
}

import SurfaceJVMTest._
class SurfaceJVMTest extends SurfaceSpec {
  test("resolve ParSeq") {
    pendingUntil("ParSeq is not available in Scala 2.13")
    // import scala.collection.parallel.ParSeq
    // check(Surface.of[ParSeq[Int]], "ParSeq[Int]")
  }

  test("resolve java util type") {
    check(Surface.of[File], "File")
    check(Surface.of[java.util.Date], "Date")
    check(Surface.of[java.time.LocalDate], "LocalDate")
    check(Surface.of[java.time.LocalDateTime], "LocalDateTime")
    check(Surface.of[java.time.Instant], "Instant")
  }

  test("resolve java enum type") {
    check(Surface.of[ChronoUnit], "ChronoUnit")
    check(Surface.of[MyChrono], "MyChrono:=ChronoUnit")
  }
}
