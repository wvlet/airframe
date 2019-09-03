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

/**
  * A trait for defining lifecycle hooks that will be called at the individual lifecycle stages
  */
trait LifeCycle extends InitLifeCycle with InjectLifeCycle with BeforeShutdownLifeCycle with StartAndShutdownLifeCycle

trait InitLifeCycle {
  def onInit: Unit
}

trait InjectLifeCycle {
  def onInject: Unit
}

trait BeforeShutdownLifeCycle {
  def beforeShutdown: Unit
}

trait StartAndShutdownLifeCycle extends StartLifeCycle with ShutdownLifeCycle

trait StartLifeCycle {
  def onStart: Unit
}

trait ShutdownLifeCycle {
  def onShutdown: Unit
}
