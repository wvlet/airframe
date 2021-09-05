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
import wvlet.airframe.tracing.DIStats

/**
  * To check the coverage of your design, set DIStats to your design.
  *
  * This is useful to see unused bindings in the design.
  */
object DI_91_Stats extends App {
  import wvlet.airframe._
  trait A {
    val b = bind[B]
  }
  trait B
  trait C

  val stats = new DIStats()
  val d = newSilentDesign
    .bind[A].toSingleton
    .bind[B].toSingleton
    .bind[C].toSingleton
    .withStats(stats) // Set stats

  d.build[A] { a =>
    //
  }

  val report = stats.coverageReportFor(d)

  /**
    * Show the design coverage and access stats.
    *
    * [coverage] design coverage: 66.7% [unused types] C [access stats] [A] init:1, inject:1 [B] init:1, inject:1
    */
  println(report)
}
