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
package wvlet.airframe.di

import wvlet.airframe.di.lifecycle.{
  LifeCycleEventHandler,
  LifeCycleManager,
  ShowDebugLifeCycleLog,
  ShowLifeCycleLog,
  AddShutdownHook
}
import wvlet.log.LogSupport

sealed trait Stage
object Stage {

  /**
    * Initialize singletons lazily
    */
  case object DEVELOPMENT extends Stage

  /**
    * Initialize singletons eagerly
    */
  case object PRODUCTION extends Stage
}

/**
  */
class SessionBuilder(
    design: Design,
    parent: Option[AirframeSession] = None,
    name: Option[String] = None,
    addShutdownHook: Boolean = true,
    lifeCycleEventHandler: LifeCycleEventHandler = LifeCycleManager.defaultLifeCycleEventHandler
) extends LogSupport {

  /**
    * @param e
    * @return
    */
  def withEventHandler(e: LifeCycleEventHandler): SessionBuilder = {
    new SessionBuilder(
      design,
      parent,
      name,
      addShutdownHook,
      // Wrap the default handler with a new LifeCycleEventHandler
      // so as not to remove the default lifecycle hooks (e.g. FILOLifeCycleHookExecutor)
      e.wraps(lifeCycleEventHandler)
    )
  }

  def withName(sessionName: String): SessionBuilder = {
    new SessionBuilder(design, parent, Some(sessionName), addShutdownHook, lifeCycleEventHandler)
  }

  def noShutdownHook: SessionBuilder = {
    new SessionBuilder(design, parent, name, false, lifeCycleEventHandler)
  }

  def build: Session = create

  def create: Session = {
    // Remove duplicate bindings in the design
    val d = design.minimize
    // Combine the lifecycle logger and event handlers
    val lifeCycleLogger =
      if (d.designOptions.enabledLifeCycleLogging.getOrElse(true)) {
        ShowLifeCycleLog
      } else {
        // Show life cycle log in debug level only
        ShowDebugLifeCycleLog
      }

    // Add a shutdown hook handler if necessary
    val lh = lifeCycleEventHandler.removeAll(AddShutdownHook)
    val eventHandler = if (addShutdownHook) {
      lh andThen AddShutdownHook
    } else {
      lh
    }

    val l       = new LifeCycleManager(lifeCycleLogger wraps eventHandler, lh)
    val stage   = d.designOptions.stage.getOrElse(Stage.DEVELOPMENT)
    val session = new AirframeSession(parent = parent, name, d, stage, l)
    debug(f"Creating a new session: ${session.name}")
    l.setSession(session)
    session.init
    session
  }
}
