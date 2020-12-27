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
package wvlet

import java.util.concurrent.ConcurrentHashMap

import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.language.implicitConversions

/**
  */
package object airframe {

  /**
    * The entry point to create a new design beginning from a blanc design
    * <code>
    * import wvlet.airframe._
    *
    * val d = design.bind[X]
    * </code>
    */
  def newDesign: Design = Design.empty

  /**
    * Create an empty design, which sends life cycle logs to debug log level
    */
  def newSilentDesign: Design = newDesign.noLifeCycleLogging

  /**
    * Inject a singleton of A
    *
    * @tparam A
    */
  def bind[A]: A = ???

  /**
    * Create a new instance of A using the provider function.
    * The lifecycle of the generated instance of A will be managed by the current session.
    */
  def bindLocal[A](provider: => A): A = ???

  /**
    * Create a new instance of A using the provider function that receives a dependency of D1.
    * The lifecycle of the generated instaance of A will be managed by the current session
    */
  def bindLocal[A, D1](provider: D1 => A): A = ???

  /**
    * Create a new instance of A using the provider function that receives dependencies of D1 and D2.
    * The lifecycle of the generated instance of A will be managed by the current session
    */
  def bindLocal[A, D1, D2](provider: (D1, D2) => A): A = ???

  /**
    * Create a new instance of A using the provider function that receives dependencies of D1, D2, and D3.
    * The lifecycle of the generated instance of A will be managed by the current session
    */
  def bindLocal[A, D1, D2, D3](provider: (D1, D2, D3) => A): A = ???

  /**
    * Create a new instance of A using the provider function that receives dependencies of D1, D2, D3, and D4.
    * The lifecycle of the generated instance of A will be managed by the current session
    */
  def bindLocal[A, D1, D2, D3, D4](provider: (D1, D2, D3, D4) => A): A = ???

  /**
    * Create a new instance of A using the provider function that receives dependencies of D1, D2, D3, D4, and D5.
    * The lifecycle of the generated instance of A will be managed by the current session
    */
  def bindLocal[A, D1, D2, D3, D4, D5](provider: (D1, D2, D3, D4, D5) => A): A = ???

  // For internal use to hold caches of factories of trait with a session
  def registerTraitFactory[A]: Surface = ???

  import scala.jdk.CollectionConverters._
  val traitFactoryCache = new ConcurrentHashMap[Surface, Session => Any].asScala
  def getOrElseUpdateTraitFactoryCache(s: Surface, factory: Session => Any): Session => Any = {
    traitFactoryCache.getOrElseUpdate(s, factory)
  }
}
