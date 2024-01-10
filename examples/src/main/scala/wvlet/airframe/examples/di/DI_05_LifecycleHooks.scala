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

import java.util.concurrent.{Executor, ExecutorService, Executors}

import wvlet.log.LogSupport

/**
  * An example of adding lifecycle hooks to the injected service
  */
object DI_05_LifecycleHooks extends App with LogSupport {
  import wvlet.airframe.*

  class MyApp(threadManager: ExecutorService)

  val d = newDesign
    .bind[ExecutorService].toInstance(Executors.newCachedThreadPool)
    .onStart { x => info(f"Started a thread manager: ${x.hashCode()}%x") }
    .onShutdown { x => info(f"Shutting down the thread manager: ${x.hashCode()}%x") }

  d.build[MyApp] { app =>
    // Thread manager will start here
  }
  // Thread manager will be shutdown here.
}
