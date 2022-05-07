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

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class AsyncTest extends AirSpec {

  private implicit val ec = defaultExecutionContext

  private val completedTests = ListBuffer.empty[String]

  private def reportCompletion(name: String): Unit = {
    synchronized {
      completedTests += name
    }
  }

  override def afterAll: Unit = {
    val completionOrders = completedTests.toSeq

    // The execution order within the same level tests is guaranteed
    completionOrders.indexOf("0") < completionOrders.indexOf("1") shouldBe true
    completionOrders.indexOf("1.1") < completionOrders.indexOf("1.2") shouldBe true
    completionOrders.indexOf("1.1.1") < completionOrders.indexOf("1.1.2") shouldBe true
  }

  test("return Future[_]") {
    Future { reportCompletion("0") }
  }

  test("nested async test") {
    test("test1.1") {
      test("test1.1.1") {
        Future { reportCompletion("1.1.1") }
      }
      test("test1.1.2") {
        Future { reportCompletion("1.1.2") }
      }

      Future { reportCompletion("1.1") }
    }

    test("test1.2") {
      Future { reportCompletion("1.2") }
    }
    Future { reportCompletion("1") }
  }

}
