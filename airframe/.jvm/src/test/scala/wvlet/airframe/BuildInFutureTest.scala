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
package wvlet.airframe

import java.util.concurrent.Executors

import wvlet.airspec.AirSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

case class Config1(port: Int = 8080)
case class Config2()

class BuildInFutureTest extends AirSpec {

  // We need to use an executor which can load applicttion classes #918.
  //
  // https://github.com/sbt/sbt/issues/5410
  private val threadPool              = Executors.newCachedThreadPool()
  private implicit val futureExecutor = ExecutionContext.fromExecutor(threadPool)

  def `Building in Future causes MISSING_DEPENDENCY` = {
    val f = Future {
      newSilentDesign.build[Config1] { config => debug(config) }
    }
    Await.result(f, Duration.Inf)
  }

  def `Building in Future causes java.lang.ClassCastException` = {
    val f = Future {
      newSilentDesign
        .bind[Config2].toInstance(Config2())
        .build[Config1] { config => debug(config) }
    }
    Await.result(f, Duration.Inf)
  }
}
