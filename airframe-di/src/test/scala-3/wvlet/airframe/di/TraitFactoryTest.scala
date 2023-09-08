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

import wvlet.airspec.AirSpec
import scala.language.experimental
import wvlet.airframe.*
import wvlet.airframe.surface.Surface

object TraitFactoryTest extends AirSpec {

  trait A

  test("register trait factory") {
    if isScala3 then {
      pending("In Scala 3.3.1, creating a new trait instance via macro is still experimental")
    }
    registerTraitFactory[A]
    traitFactoryCache.get(Surface.of[A]) shouldBe defined
  }

  trait B {
    def hello: Unit
  }

  test("do not create trait factory for abstract classes") {
    registerTraitFactory[B]
    // ---
    traitFactoryCache.get(Surface.of[B]) shouldBe empty
  }

//  test("do not create trait for a local type") {
//    trait Local
//    registerTraitFactory[Local]
//    traitFactoryCache.get(Surface.of[Local]) shouldBe empty
//  }

  class C

  test("should not register trait factory for a simple class") {
    registerTraitFactory[C]
    traitFactoryCache.get(Surface.of[B]) shouldBe empty
  }

  class D(i: Int)

  test("do not creat trait factroy for a class without any public constructor") {
    registerTraitFactory[D]
    traitFactoryCache.get(Surface.of[B]) shouldBe empty
  }

}
