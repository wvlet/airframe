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
import java.util.concurrent.atomic.AtomicBoolean

import wvlet.airspec.AirSpec

import scala.concurrent.{Future, Promise}

/**
  *
  */
class AsyncTest extends AirSpec {
  scalaJsSupport

  private implicit val sc = scala.concurrent.ExecutionContext.global

  private val checker  = new AtomicBoolean(false)
  private val checker2 = new AtomicBoolean(false)
  private val checker3 = new AtomicBoolean(false)

  override protected def afterAll: Unit = {
    checker.get shouldBe true
    checker2.get shouldBe true
    checker3.get shouldBe true
  }

  test("support Future[X] in test(...)") {
    Future("hello")
      .map { x =>
        s"${x} world"
      }.map { x =>
        checker.set(true)
        x shouldBe "hello world"
      }
  }

  def `support future return type`: Future[Unit] = {
    Future {
      checker2.set(true)
      true shouldBe true
    }
  }

  def `support Promise[_]` = {
    val p = Promise.apply[String]()
    p.completeWith(Future {
      "hello promise"
    })
    p.future.map { x =>
      checker3.set(true)
      x shouldBe "hello promise"
    }
  }

  def `handle exception in Future` = {
    Future {
      pendingUntil("Pending inside Future[_] test")
    }
  }

  def `handle assertion error in Future` = {
    Future {
      ignore("Ignore inside Future[_] test")
    }
  }
}
