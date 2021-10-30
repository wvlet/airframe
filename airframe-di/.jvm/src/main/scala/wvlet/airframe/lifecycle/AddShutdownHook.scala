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
package wvlet.airframe.lifecycle

import wvlet.log.AirframeLogManager

import scala.collection.mutable
import scala.sys.ShutdownHookThread

/**
  */
object AddShutdownHook extends LifeCycleEventHandler {
  private val shutdownHooks = new mutable.WeakHashMap[LifeCycleManager, ShutdownHookThread]()

  private def removeShutdownHooksFor(lifeCycleManager: LifeCycleManager): Unit = {
    synchronized {
      shutdownHooks.get(lifeCycleManager).map { h =>
        // Properly unregister shutdown hooks
        // This will be a workaround for sbt-1.3.0-RC2 https://github.com/sbt/sbt/issues/4794 (user class will not be visible at sbt shutdown)
        if (h != null) {
          if (!h.isAlive) {
            // Remove the shutdown hook if JVM shutdown is not yet started
            h.remove()
          }
        }
        shutdownHooks.remove(lifeCycleManager)
      }
      // Resetting the logger when all LifeCycleManagers have terminated
      if (shutdownHooks.isEmpty) {
        // AirframeLogManager.resetFinally
      }
    }
  }

  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    synchronized {
      // Remember the registered shutdown hooks
      shutdownHooks.getOrElseUpdate(
        lifeCycleManager, {
          sys.addShutdownHook {
            lifeCycleManager.shutdown
            removeShutdownHooksFor(lifeCycleManager)
          }
        }
      )
    }
  }

  override def afterShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    // Unregister shutdown hooks
    removeShutdownHooksFor(lifeCycleManager)
  }
}
