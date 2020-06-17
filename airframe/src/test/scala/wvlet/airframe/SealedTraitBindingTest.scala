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
import example.SealedTrait.Adt
import wvlet.airspec.AirSpec

/**
  */
object SealedTraitBindingTest extends AirSpec {
  trait Service {
    // This code compilation may fail when using Mill.
    val adt = bind[Adt]
  }

  test("compile test") {
    // Just need to check the compilation failure
    val design = Design.newSilentDesign
      .bind[Adt].toInstance(Adt.Foo)

    design.build[Service] { s => debug(s.adt) }
  }
}
