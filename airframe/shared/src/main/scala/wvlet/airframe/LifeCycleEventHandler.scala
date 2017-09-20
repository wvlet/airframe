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

import wvlet.surface.Surface

/**
  *
  */
trait LifeCycleEventHandler {
  def onInit(lifeCycleManager: LifeCycleManager, t: Surface, injectee: AnyRef) {}
  def beforeStart(lifeCycleManager: LifeCycleManager) {}
  def afterStart(lifeCycleManager: LifeCycleManager) {}
  def beforeShutdown(lifeCycleManager: LifeCycleManager) {}
  def afterShutdown(lifeCycleManager: LifeCycleManager) {}

  def andThen(next: LifeCycleEventHandler): LifeCycleEventHandler = new LifeCycleEventHandlerChain(this, next)
  def wraps(child: LifeCycleEventHandler): LifeCycleEventHandler  = new LifeCycleEventHandlerPair(this, child)
}

class LifeCycleEventHandlerChain(prev: LifeCycleEventHandler, next: LifeCycleEventHandler) extends LifeCycleEventHandler {
  override def onInit(lifeCycleManager: LifeCycleManager, t: Surface, injectee: AnyRef): Unit = {
    prev.onInit(lifeCycleManager, t, injectee)
    next.onInit(lifeCycleManager, t, injectee)
  }
  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    prev.beforeStart(lifeCycleManager)
    next.beforeStart(lifeCycleManager)
  }
  override def afterStart(lifeCycleManager: LifeCycleManager): Unit = {
    prev.afterStart(lifeCycleManager)
    next.afterStart(lifeCycleManager)
  }
  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    prev.beforeShutdown(lifeCycleManager)
    next.beforeShutdown(lifeCycleManager)
  }
  override def afterShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    prev.afterShutdown(lifeCycleManager)
    next.afterShutdown(lifeCycleManager)
  }
}

class LifeCycleEventHandlerPair(parent: LifeCycleEventHandler, child: LifeCycleEventHandler) extends LifeCycleEventHandler {
  override def onInit(lifeCycleManager: LifeCycleManager, t: Surface, injectee: AnyRef): Unit = {
    parent.onInit(lifeCycleManager, t, injectee)
    child.onInit(lifeCycleManager, t, injectee)
  }
  override def beforeStart(lifeCycleManager: LifeCycleManager): Unit = {
    parent.beforeStart(lifeCycleManager)
    child.beforeStart(lifeCycleManager)
  }
  override def afterStart(lifeCycleManager: LifeCycleManager): Unit = {
    child.afterStart(lifeCycleManager)
    parent.afterStart(lifeCycleManager)
  }
  override def beforeShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    parent.beforeShutdown(lifeCycleManager)
    child.beforeShutdown(lifeCycleManager)
  }
  override def afterShutdown(lifeCycleManager: LifeCycleManager): Unit = {
    child.afterShutdown(lifeCycleManager)
    parent.afterShutdown(lifeCycleManager)
  }
}
