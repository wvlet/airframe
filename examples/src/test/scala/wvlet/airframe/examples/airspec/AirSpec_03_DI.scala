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
package wvlet.airframe.examples.airspec

import wvlet.airframe.Design
import wvlet.airspec.AirSpec

/**
  */
object AirSpec_03_DI extends AirSpec {
  case class Config(port: Int)
  class MyService(val config: Config)

  protected override def design: Design = {
    import wvlet.airframe._

    val d = newDesign
      .bind[Config].toInstance(Config(port = 8080))
      .bind[MyService].toSingleton
      .onStart(x => info(s"Starting server at port:${x.config.port}"))
      .onShutdown(x => info(s"Stopping server at port:${x.config.port}"))
    d
  }

  // Injecting a service using DI
  test("test service") { (service: MyService) =>
    info(s"service id: ${service.hashCode()}")
    service.config.port shouldBe 8080
  }

  // The same service instance will be passed
  test("test service2") { (service: MyService) =>
    info(s"service id: ${service.hashCode()}")
    service.config.port shouldBe 8080
  }
}
