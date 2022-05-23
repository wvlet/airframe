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
package examples

import wvlet.airspec.AirSpec
import wvlet.airspec.spi.AssertionFailure

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}

class AsyncFailureTest extends AirSpec {

  private implicit val ec: ExecutionContext = defaultExecutionContext

  private val isFailureRecorded        = new AtomicBoolean(false)
  private val isSucceedingTestExecuted = new AtomicBoolean(false)

  override def afterAll: Unit = {
    isFailureRecorded.get() shouldBe true
    isSucceedingTestExecuted.get() shouldBe true
  }

  test("return async failure") {
    Future {
      // This test should fail
      1 shouldBe 0
    }
      .recover {
        case e: AssertionFailure =>
          // ok
          isFailureRecorded.set(true)
        case _ =>
          fail("cannot reach here")
      }
  }

  test("run other tests") {
    true shouldBe true
    isSucceedingTestExecuted.set(true)
  }

}
