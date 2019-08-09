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
package wvlet.airframe.spec
import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airframe.{Design, _}
import wvlet.log.LogSupport

case class MyServerConfig(name: String)

trait MyServer extends LogSupport {
  val config = bind[MyServerConfig]

  @PostConstruct
  def start: Unit = {
    info(f"Starting ${config.name}: ${this.hashCode()}%x")
  }

  @PreDestroy
  def stop: Unit = {
    info(f"Stopping ${config.name}: ${this.hashCode()}%x")
  }
}

/**
  *
  */
trait CustomSpec extends AirSpec with LogSupport {
  override def beforeAll(design: Design): Design = {
    design
      .bind[MyServer].toSingleton
      .bind[MyServerConfig].toInstance(MyServerConfig("A"))
  }
}

class MyServerSpec extends CustomSpec {
  def test1(server: MyServer): Unit = {
    info(s"run test1")
    assert(server.config.name == "A")
  }

  def test2(server: MyServer): Unit = {
    info(s"run test2")
  }

  def test3(session: Session): Unit = {
    info(s"run test3")
    val server = session.build[MyServer]
  }
}

class MyServer2Spec extends CustomSpec {
  override protected def before(design: Design): Design = {
    design.bind[MyServerConfig].toInstance(MyServerConfig("B"))
  }

  def test4(server: MyServer): Unit = {
    info("run test4")
    assert(server.config.name == "B")
  }
}
