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

import wvlet.log.LogSupport

/**
  * Design objects are composable
  */
object DI_09_ComposingDesigns extends App with LogSupport {
  import wvlet.airframe.*

  val d1 = newSilentDesign
    .bind[Int].toInstance(10)
    .bind[String].toInstance("Hello!")

  val d2 = newSilentDesign
    .bind[String].toInstance("Good Night!")

  // Combining designs
  val d3 = d1 + d2

  d3.withSession { session =>
    val i = session.build[Int]    // 10
    val s = session.build[String] // Good Night!
    info(s"${i} ${s}")
  }

  // Adding designs is not commutative because preceding bindings for the same types will be overwritten.
  val d4 = d2 + d1
  d4.withSession { session =>
    val i = session.build[Int]    // 10
    val s = session.build[String] // Hello!
    info(s"${i} ${s}")
  }
}
