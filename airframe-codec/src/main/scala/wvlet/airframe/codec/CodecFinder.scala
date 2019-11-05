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
trait CodecFinder {
  def findCodec(
      factory: MessageCodecFactory,
      seenSet: Set[Surface] = Set.empty
  ): PartialFunction[Surface, MessageCodec[_]]

  def orElse(other: CodecFinder): CodecFinder = CodecFinder.OrElse(this, other)
}

object CodecFinder {
  case class OrElse(a: CodecFinder, b: CodecFinder) extends CodecFinder {
    override def findCodec(
        factory: MessageCodecFactory,
        seenSet: Set[Surface]
    ): PartialFunction[Surface, MessageCodec[_]] = {
      a.findCodec(factory, seenSet).orElse(b.findCodec(factory, seenSet))
    }
  }

  def defaultCodecFinder = new CodecFinder {
    override def findCodec(
        factory: MessageCodecFactory,
        seenSet: Set[Surface]
    ): PartialFunction[Surface, MessageCodec[_]] = {
      case g: GenericSurface
          if classOf[Product].isAssignableFrom(g.rawType) && g.rawType.getName.startsWith("scala.Tuple") =>
        TupleCodec(g.typeArgs.map(factory.ofSurface(_, seenSet)))
      case g: GenericSurface if classOf[Seq[_]].isAssignableFrom(g.rawType) =>
        // Seq[A]
        val elementSurface = factory.ofSurface(g.typeArgs(0), seenSet)
        g match {
          case g1: GenericSurface if classOf[IndexedSeq[_]].isAssignableFrom(g1.rawType) =>
            new CollectionCodec.IndexedSeqCodec(g1.typeArgs(0), elementSurface)
          case g1: GenericSurface if classOf[List[_]].isAssignableFrom(g1.rawType) =>
            new CollectionCodec.ListCodec(g1.typeArgs(0), elementSurface)
          case _ =>
            new CollectionCodec.SeqCodec(g.typeArgs(0), elementSurface)
        }
      case g: GenericSurface if classOf[Map[_, _]].isAssignableFrom(g.rawType) =>
        CollectionCodec.MapCodec(
          factory.ofSurface(g.typeArgs(0), seenSet),
          factory.ofSurface(g.typeArgs(1), seenSet)
        )
      case g: GenericSurface if classOf[java.util.Collection[_]].isAssignableFrom(g.rawType) =>
        val elementSurface = factory.ofSurface(g.typeArgs(0), seenSet)
        CollectionCodec.JavaListCodec(elementSurface)
      case g: GenericSurface if classOf[java.util.Map[_, _]].isAssignableFrom(g.rawType) =>
        CollectionCodec.JavaMapCodec(
          factory.ofSurface(g.typeArgs(0), seenSet),
          factory.ofSurface(g.typeArgs(1), seenSet)
        )
    }
  }
}
