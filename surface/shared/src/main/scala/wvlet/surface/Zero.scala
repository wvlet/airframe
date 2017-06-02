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

package wvlet.surface

import wvlet.log.LogSupport

import scala.reflect.ClassTag
import scala.util.Try

/**
  * Create a default instance (zero) from Surface
  */
object Zero extends LogSupport {

  type ZeroValueFactory = PartialFunction[Surface, Any]
  type SurfaceFilter = PartialFunction[Surface, Surface]

  private def isPrimitive: SurfaceFilter = {
    case p if p.isPrimitive => p
  }
  private def isGenericWithTypeArgs: SurfaceFilter = {
    case g: GenericSurface if g.typeArgs.length > 0 => g
  }

  private def zeroOfPrimitives: ZeroValueFactory = isPrimitive andThen {
    case Primitive.String => ""
    case Primitive.Boolean => false
    case Primitive.Short => 0.toShort
    case Primitive.Int => 0
    case Primitive.Long => 0L
    case Primitive.Float => 0f
    case Primitive.Double => 0.0
    case Primitive.Byte => 0.toByte
    case Primitive.Unit => null
  }

  private def zeroOfArray: ZeroValueFactory = {
    case ArraySurface(cl, elementSurface) =>
      ClassTag(elementSurface.rawType).newArray(0)
  }

  private def zeroOfSpecialType: ZeroValueFactory = {
    case s if s.isOption => None
    case s if s.rawType == classOf[Nothing] ||
      s.rawType == classOf[AnyRef] ||
      s.rawType == classOf[Any]
    => null
  }

  private def zeroOfScalaCollections: ZeroValueFactory = isGenericWithTypeArgs andThen {
    case g if classOf[Seq[_]].isAssignableFrom(g.rawType) =>
      Seq.empty
    case g if classOf[Map[_, _]].isAssignableFrom(g.rawType) =>
      Map.empty
    case g if classOf[Set[_]].isAssignableFrom(g.rawType) =>
      Set.empty
  }

  private def zeroOfTuple: ZeroValueFactory = {
    case t: TupleSurface if t.objectFactory.isDefined =>
      val factory = t.objectFactory.get
      val args = t.typeArgs.map(s => zeroOf(s))
      factory.newInstance(args)
  }

  private def zeroOfInstantiatable: ZeroValueFactory = {
    case t if t.objectFactory.isDefined =>
      val factory = t.objectFactory.get
      val args = t.params.map(x => zeroOf(x.surface))
      factory.newInstance(args)
  }

  private def fallBack: ZeroValueFactory = {
    case other =>
      null
  }

  private val factory: ZeroValueFactory =
    zeroOfPrimitives orElse
      zeroOfArray orElse
      zeroOfTuple orElse
      zeroOfSpecialType orElse
      zeroOfScalaCollections orElse
      zeroOfInstantiatable orElse
      fallBack

  def zeroOf(surface: Surface): Any = {
    // Dealias function resolves the actual types of aliased surfaces, tagged surfaces etc.
    val zero = factory.apply(surface.dealias)
    zero
  }
}
