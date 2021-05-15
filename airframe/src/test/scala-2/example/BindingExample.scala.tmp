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
package example

import wvlet.airframe._
import wvlet.log.LogSupport



object BindingExample {
  trait A
  class AImpl extends A
  case class B(id:Int)

  class S
  class ES

  case class D1(v:Int = 0)
  case class D2(v:Int = 0)
  case class D3(v:Int = 0)
  case class P(d1:D1 = D1(), d2:D2 = D2(), d3:D3 = D3())
  def provider(d1:D1, d2:D2, d3:D3) : P = P(d1, d2, d3)
}

import BindingExample._

trait BindingExample {
  val a = bind[A]  // Inject A
  val b = bind[B]  // Inject B

  val s = bindSingleton[S] // Inject S as a singleton

  val p0 = bind { P() } // Inject P using the provider
  val p1 = bind { d1:D1 => P(d1) } // Inject D1 to create P
  val p2 = bind { (d1:D1, d2:D2) => P(d1, d2) } // Inject D1 and D2 to create P
  val p3 = bind { (d1:D1, d2:D2, d3:D3) => P(d1, d2, d3) } // Inject D1, D2 and D3

  val pd = bind { provider _ } // Inject D1, D2 and D3 to call the provider function

  val ps = bindSingleton { provider _ } // Create a singleton using a provider
}

object DesignExample {
  // If you define multiple bindings to the same type, the last one will be used.

  val design =
    newDesign                      // Create an empty design
    .bind[A].to[AImpl]             // Concrete class binding
    .bind[B].toInstance(new B(1))  // Instance binding
    .bind[S].toSingleton           // S will be a singleton within the session
    .bind[ES].toEagerSingleton     // ES will be initialized as a singleton at session start
    .bind[D1].toInstance(D1(1))    // Bind D1 to a concrete instance D1(1)
    .bind[D2].toInstance(D2(2))    // Bind D2 to a concrete instance D2(2)
    .bind[D3].toInstance(D3(3))    // Bind D3 to a cocreete instance D3(3)
    .bind[P].toProvider{ d1:D1 => P(d1) } // Create P by resolving D1 from the design
    .bind[P].toProvider{ (d1:D1, d2:D2) => P(d1, d2) } // Resolve D1 and D2
    .bind[P].toProvider{ provider _ }  // Use a function as a provider. D1, D2 and D3 will be resolved from the design
    .bind[P].toSingletonProvider{ d1:D1 => P(d1) } // Create a singleton using the provider function
    .bind[P].toEagerSingletonProvider{ d1:D1 => P(d1) } // Create an eager singleton using the provider function
}


trait Server {
  def init() {}
  def start() {}
  def stop() {}
}

trait MyServerService {
  val service = bind[Server].withLifeCycle(
    init = { _.init }, // Called when injected
    start = { _.start }, // Called when sesion.start is called
    shutdown = { _.stop } // Called when session.shutdown is called
  )
}
