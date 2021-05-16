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
package wvlet.airspec

import java.util.concurrent.atomic.AtomicInteger

import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airframe.{Design, _}
import wvlet.log.LogSupport

case class MyServerConfig(name: String)

class MyServer(val config: MyServerConfig, val counter: AtomicInteger) extends LogSupport {

  @PostConstruct
  def start: Unit = {
    debug(f"Starting ${config.name}: ${this.hashCode()}%x")
    counter.incrementAndGet()
  }

  @PreDestroy
  def stop: Unit = {
    debug(f"Stopping ${config.name}: ${this.hashCode()}%x")
  }
}

/**
  */
trait CustomSpec extends AirSpec with LogSupport {
  protected val serverLaunchCounter = new AtomicInteger(0)

  protected override def design: Design = {
    Design.newDesign
      .bind[MyServer].toSingleton
      .bind[MyServerConfig].toInstance(MyServerConfig("A"))
      .bind[AtomicInteger].toInstance(serverLaunchCounter)
  }
}

class MyServerSpec extends CustomSpec {
  // MyServer will be shared by the all test cases
  test("test1") { (server: MyServer) =>
    debug(s"run test1")
    assert(server.config.name == "A")
  }

  test("test2") { (server: MyServer) =>
    debug(s"run test2")
  }

  test("test3") { (session: Session) =>
    debug(s"run test3")
    val server = session.build[MyServer]
  }

  override protected def afterAll: Unit = {
    assert(serverLaunchCounter.get() == 1)
  }
}

class MyServer2Spec extends CustomSpec {
  protected override def localDesign: Design = {
    Design.newDesign
      .bind[MyServerConfig].toInstance(MyServerConfig("B"))
      // By adding this local design, the server will be a test case local
      .bind[MyServer].toSingleton
  }

  test("test4") { (server: MyServer) =>
    debug("run test4")
    assert(server.config.name == "B")
  }

  test("test5") { (server: MyServer) =>
    debug("run test5")
    assert(server.config.name == "B")
  }

  override protected def afterAll: Unit = {
    assert(serverLaunchCounter.get() == testDefinitions.size)
  }
}
