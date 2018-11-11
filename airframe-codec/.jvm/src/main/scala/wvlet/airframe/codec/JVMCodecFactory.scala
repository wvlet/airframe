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
package wvlet.airframe.codec
import wvlet.airframe.codec.CollectionCodec.{IndexedSeqCodec, ListCodec, SeqCodec}
import wvlet.airframe.codec.ScalaStandardCodec.TupleCodec
import wvlet.airframe.surface.reflect.ReflectTypeUtil
import wvlet.airframe.surface.{GenericSurface, Surface}

/**
  *
  */
object JVMCodecFactory extends CodecFinder {

  def findCodec(factory: MessageCodecFactory,
                seenSet: Set[Surface] = Set.empty): PartialFunction[Surface, MessageCodec[_]] = {
    case s if ReflectTypeUtil.isTuple(s.rawType) =>
      TupleCodec(s.typeArgs.map(x => factory.ofSurface(x)))
    case g: GenericSurface if ReflectTypeUtil.isSeq(g.rawType) =>
      // Seq[A]
      val elementSurface = factory.ofSurface(g.typeArgs(0), seenSet)
      if (ReflectTypeUtil.isIndexedSeq(g.rawType)) {
        new IndexedSeqCodec(g.typeArgs(0), elementSurface)
      } else if (ReflectTypeUtil.isList(g.rawType)) {
        new ListCodec(g.typeArgs(0), elementSurface)
      } else {
        new SeqCodec(g.typeArgs(0), elementSurface)
      }
    case g: GenericSurface if ReflectTypeUtil.isJavaColleciton(g.rawType) =>
      CollectionCodec.JavaListCodec(factory.ofSurface(g.typeArgs(0), seenSet))
    case g: GenericSurface if ReflectTypeUtil.isMap(g.rawType) =>
      // Map[A,B]
      CollectionCodec.MapCodec(factory.ofSurface(g.typeArgs(0), seenSet), factory.ofSurface(g.typeArgs(1), seenSet))
    case g: GenericSurface if ReflectTypeUtil.isJavaMap(g.rawType) =>
      // Map[A,B]
      CollectionCodec.JavaMapCodec(factory.ofSurface(g.typeArgs(0), seenSet), factory.ofSurface(g.typeArgs(1), seenSet))
    case s if ReflectTypeUtil.isTuple(s.rawType) =>
      // Tuple
      TupleCodec(s.typeArgs.map(x => factory.ofSurface(x, seenSet)))
    case s if ReflectTypeUtil.hasStringUnapplyConstructor(s) =>
      new StringUnapplyCodec(s)
  }

}
