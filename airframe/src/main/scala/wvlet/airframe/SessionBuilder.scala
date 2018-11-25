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
  *
  */
class SessionBuilder(design: Design,
                     name: Option[String] = None,
                     lifeCycleEventHandler: LifeCycleEventHandler = LifeCycleManager.mandatoryObjectLifeCycleHandler)
    extends LogSupport {

  /**
    * @param e
    * @return
    */
  def withEventHandler(e: LifeCycleEventHandler): SessionBuilder = {
    new SessionBuilder(design, name, e.wraps(lifeCycleEventHandler))
  }

  def withName(sessionName: String): SessionBuilder = {
    new SessionBuilder(design, Some(sessionName), lifeCycleEventHandler)
  }

  def create: Session = {
    // Combine the lifecycle logger and event handlers
    val lifeCycleLogger =
      if (design.designOptions.enabledLifeCycleLogging) {
        ShowLifeCycleLog
      } else {
        // Show life cycle log in debug level only
        ShowDebugLifeCycleLog
      }
    val eventHandler = lifeCycleLogger wraps lifeCycleEventHandler
    val l            = new LifeCycleManager(eventHandler)
    val session      = new AirframeSession(parent = None, name, design, design.designOptions.stage, l)
    debug(f"Creating a new session: ${session.name}")
    l.setSession(session)
    session.init
    session
  }
}
