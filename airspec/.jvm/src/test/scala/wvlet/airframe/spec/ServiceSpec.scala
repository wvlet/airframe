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

import wvlet.airframe.Design
import wvlet.log.LogSupport
import javax.annotation._

case class ServiceConfig(port: Int)

class Service(config: ServiceConfig) extends LogSupport {
  @PostConstruct
  def start: Unit = {
    info(s"Starting a server at ${config.port}")
  }

  @PreDestroy
  def end: Unit = {
    info(s"Stopping the server at ${config.port}")
  }
}

class ServiceSpec extends AirSpec with LogSupport {
  override protected def configure(design: Design): Design = {
    design
      .bind[Service].toSingleton
      .bind[ServiceConfig].toInstance(ServiceConfig(port = 8080))
  }

  def test1(service: Service): Unit = {
    info(s"test1: server id: ${service.hashCode}")
  }

  def test2(service: Service): Unit = {
    info(s"test2: server id: ${service.hashCode}")
  }
}
