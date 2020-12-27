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

import wvlet.airframe.surface.Surface
import scala.quoted._

private[wvlet] object AirframeDIMacros {
  def shouldGenerateTraitOf[A](using quotes:Quotes, tpe: Type[A]): Boolean = {
    import quotes.reflect._
    val t = TypeRepr.of(using tpe)
    val ts = t.typeSymbol

    // Has a publid default constructor that can be called without any argument?
    val hasPublicDefaultConstructor = {
      val pc = ts.primaryConstructor
      pc.paramSymss.size == 1 && pc.paramSymss(0).size == 0
    }
    val hasAbstractMethods = {
      ts.memberMethods.exists(m => m.flags.is(Flags.Abstract))
    }
    val isTaggedType = ts.fullName.startsWith("wvlet.airframe.surface.tag.")
    val isSealedType = ts.flags.is(Flags.Sealed)
    val shouldInstantiateTrait = if(!ts.flags.is(Flags.Static)) {
      // = Non static type
      // If X is non static type (= local class or trait),
      // we need to instantiate it first in order to populate its $outer variables

      // We cannot instantiate path-dependent types
      if(ts.fullName.contains("#")) {
        false
      } 
      else {
        hasPublicDefaultConstructor
      }
    } else if (ts.flags.is(Flags.Abstract)) {
      // = Abstract type
      // We cannot build abstract type X that has abstract methods, so bind[X].to[ConcreteType]
      // needs to be found in the design

      // If there is no abstract methods, it might be a trait without any method
      !hasAbstractMethods
    } else {
      // We cannot instantiate any trait or class without the default constructor
      // So binding needs to be found in the Design.
      hasPublicDefaultConstructor
    }

    // Tagged type or sealed class binding should be found in Design
    !isTaggedType && !isSealedType && shouldInstantiateTrait
  }
}