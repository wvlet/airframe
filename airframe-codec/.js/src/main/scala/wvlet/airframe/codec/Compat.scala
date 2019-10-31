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
import wvlet.airframe.codec.ScalaStandardCodec.TupleCodec
import wvlet.airframe.surface.{GenericSurface, Surface}

/**
  *
  */
object Compat {

  def codecFinder: CodecFinder                              = JSCodecFinger
  def platformSpecificCodecs: Map[Surface, MessageCodec[_]] = Map.empty

  object JSCodecFinger extends CodecFinder {
    override def findCodec(
        factory: MessageCodecFactory,
        seenSet: Set[Surface]
    ): PartialFunction[Surface, MessageCodec[_]] = {
      case g: GenericSurface
          if g.rawType.getName.startsWith("scala.Tuple") && classOf[Product].isAssignableFrom(g.rawType) =>
        TupleCodec(g.typeArgs.map(factory.ofSurface(_, seenSet)))
      case g: GenericSurface if classOf[IndexedSeq[_]].isAssignableFrom(g.rawType) =>
        new CollectionCodec.IndexedSeqCodec(g, factory.ofSurface(g.typeArgs(0), seenSet))
      case g: GenericSurface if classOf[List[_]].isAssignableFrom(g.rawType) =>
        new CollectionCodec.ListCodec(g, factory.ofSurface(g.typeArgs(0), seenSet))
      case g: GenericSurface if classOf[Seq[_]].isAssignableFrom(g.rawType) =>
        new CollectionCodec.SeqCodec(g, factory.ofSurface(g.typeArgs(0), seenSet))
      case g: GenericSurface if classOf[Map[_, _]].isAssignableFrom(g.rawType) =>
        CollectionCodec.MapCodec(factory.ofSurface(g.typeArgs(0), seenSet), factory.ofSurface(g.typeArgs(1), seenSet))
//      case other =>
//        throw new UnsupportedOperationException(s"MessageCodec for ${other} is not found")
    }
  }
}
