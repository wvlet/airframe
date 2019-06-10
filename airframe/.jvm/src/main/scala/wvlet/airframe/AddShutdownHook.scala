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

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import wvlet.log.AirframeLogManager

/**
  *
  */
object AddShutdownHook extends LifeCycleEventHandler {

  private val registered = new AtomicInteger(0)

  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    registered.incrementAndGet()

    // A workaround for https://github.com/sbt/sbt/issues/4794 (user class will not be visible at sbt shutdown)
    if (!sys.props.get("AIRFRAME_SPEC").isDefined) {
      sys.addShutdownHook {
        lifeCycleManager.shutdown

        if (registered.decrementAndGet() <= 0) {
          // Resetting the logger when all lifecycle have terminated
          AirframeLogManager.resetFinally
        }
      }
    }
  }
}
