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
import wvlet.surface.Surface

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
                     stage: Stage = Stage.DEVELOPMENT,
                     lifeCycleEventHandler: LifeCycleEventHandler = LifeCycleManager.mandatoryObjectLifeCycleHandler,
                     lifeCycleLogger: Option[LifeCycleEventHandler] = Some(ShowLifeCycleLog))
    extends LogSupport {

  /**
    * @param e
    * @return
    */
  def withEventHandler(e: LifeCycleEventHandler): SessionBuilder = {
    new SessionBuilder(design, name, stage, e.wraps(lifeCycleEventHandler), lifeCycleLogger)
  }

  def withoutLifeCycleLog: SessionBuilder = {
    new SessionBuilder(design, name, stage, lifeCycleEventHandler, None)
  }

  def withLifeCycleLogger(customLifeCycleLogger: LifeCycleEventHandler): SessionBuilder = {
    new SessionBuilder(design, name, stage, lifeCycleEventHandler, Some(customLifeCycleLogger))
  }

  def withName(sessionName: String): SessionBuilder = {
    new SessionBuilder(design, Some(sessionName), stage, lifeCycleEventHandler, lifeCycleLogger)
  }

  def withProductionStage: SessionBuilder = {
    new SessionBuilder(design, name, Stage.PRODUCTION, lifeCycleEventHandler, lifeCycleLogger)
  }

  def create: Session = {
    // Override preceding bindings
    val effectiveBindings = for ((key, lst) <- design.binding.groupBy(_.from)) yield {
      lst.last
    }
    val keyIndex: Map[Surface, Int] = design.binding.map(_.from).zipWithIndex.map(x => x._1 -> x._2).toMap
    val sortedBindings              = effectiveBindings.toSeq.sortBy(x => keyIndex(x.from))

    // Combine the lifecycle logger and event handlers
    val eventHandler =
      lifeCycleLogger
        .map(_.wraps(lifeCycleEventHandler))
        .getOrElse(lifeCycleEventHandler)

    val l       = new LifeCycleManager(eventHandler)
    val session = new AirframeSession(name, sortedBindings, stage, l)
    debug(f"Creating a new session: ${session.name}")
    l.setSession(session)
    session.init
    session
  }
}
