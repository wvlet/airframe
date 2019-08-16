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
  *
  */
object AirSpec_03_DI extends AirSpec {

  case class Config(port: Int)
  class MyService(val config: Config)

  override protected def configure(design: Design): Design = {
    design
      .bind[Config].toInstance(Config(port = 8080))
      .bind[MyService].toSingleton
  }

  // Injecting a service using DI
  def test(service: MyService): Unit = {
    info(s"service id: ${service.hashCode()}")
    service.config.port shouldBe 8080
  }

  // The same service instance will be passed
  def test2(service: MyService): Unit = {
    info(s"service id: ${service.hashCode()}")
    service.config.port shouldBe 8080
  }
}
