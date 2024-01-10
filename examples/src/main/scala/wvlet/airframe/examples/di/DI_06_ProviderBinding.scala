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
package wvlet.airframe.examples.di

import java.util.concurrent.{ExecutorService, Executors}
import wvlet.log.LogSupport

/**
  * Provider binding is useful to build objects by using dependencies defined in Design.
  */
object DI_06_ProviderBinding extends App with LogSupport {
  import wvlet.airframe.*

  case class MyAppConfig(numThreads: Int = 5)

  class MyApp(threadManager: ExecutorService) extends LogSupport {
    def run: Unit = {
      threadManager.submit(new Runnable {
        override def run(): Unit = {
          logger.info("Hello Provider!")
        }
      })
    }
  }

  val d = newSilentDesign
    .bind[MyAppConfig].toInstance(MyAppConfig(numThreads = 2))
    .bind[ExecutorService].toProvider { (config: MyAppConfig) =>
      info(s"config: numThreads = ${config.numThreads}")
      // Create a thread manager using the given config
      Executors.newFixedThreadPool(config.numThreads)
    }
    .onShutdown(_.shutdownNow())

  d.build[MyApp] { app => app.run }
}
