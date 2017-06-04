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

package wvlet.surface

import java.time.temporal.ChronoUnit
import java.io.File
import scala.collection.parallel.ParSeq
import wvlet.surface

object SurfaceJVMTest {
  type MyChrono = ChronoUnit
}

import SurfaceJVMTest._
class SurfaceJVMTest extends SurfaceSpec {

  "SurfaceJVM" should {
    "resolve ParSeq" in {
      check(surface.of[ParSeq[Int]],"ParSeq[Int]")
    }

    "resolve java util type" in {
      check(surface.of[File],"File")
      check(surface.of[java.util.Date],"Date")
      check(surface.of[java.time.LocalDate],"LocalDate")
      check(surface.of[java.time.LocalDateTime],"LocalDateTime")
      check(surface.of[java.time.Instant], "Instant")
    }

    "resolve java enum type" in {
      check(surface.of[ChronoUnit], "ChronoUnit")
      check(surface.of[MyChrono], "MyChrono:=ChronoUnit")
    }
  }
}
