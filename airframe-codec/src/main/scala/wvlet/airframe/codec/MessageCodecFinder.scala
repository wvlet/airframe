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
import wvlet.airframe.codec.ScalaStandardCodec.{EitherCodec, OptionCodec, TupleCodec}
import wvlet.airframe.surface.{Alias, EnumSurface, GenericSurface, Surface, Union}

/**
  */
trait MessageCodecFinder {
  def findCodec(
      factory: MessageCodecFactory,
      seenSet: Set[Surface] = Set.empty
  ): PartialFunction[Surface, MessageCodec[_]]

  def orElse(other: MessageCodecFinder): MessageCodecFinder = MessageCodecFinder.OrElse(this, other)
}

object MessageCodecFinder {
  object empty extends MessageCodecFinder {
    override def findCodec(
        factory: MessageCodecFactory,
        seenSet: Set[Surface]
    ): PartialFunction[Surface, MessageCodec[_]] = PartialFunction.empty
  }

  private[codec] case class OrElse(a: MessageCodecFinder, b: MessageCodecFinder) extends MessageCodecFinder {
    override def findCodec(
        factory: MessageCodecFactory,
        seenSet: Set[Surface]
    ): PartialFunction[Surface, MessageCodec[_]] = {
      a.findCodec(factory, seenSet).orElse(b.findCodec(factory, seenSet))
    }
  }

  def newCodecFinder(codecTable: Map[Surface, MessageCodec[_]]): MessageCodecFinder =
    new MessageCodecFinder {
      override def findCodec(
          factory: MessageCodecFactory,
          seenSet: Set[Surface]
      ): PartialFunction[Surface, MessageCodec[_]] = {
        case s: Surface if codecTable.contains(s) => codecTable(s)
      }
    }

  val defaultKnownCodecs: Map[Surface, MessageCodec[_]] = {
    StandardCodec.standardCodec ++
      MetricsCodec.metricsCodec ++
      Compat.platformSpecificCodecs
  }

  object defaultMessageCodecFinder extends MessageCodecFinder {
    override def findCodec(
        factory: MessageCodecFactory,
        seenSet: Set[Surface]
    ): PartialFunction[Surface, MessageCodec[_]] = {
      // Known codecs
      case s if defaultKnownCodecs.contains(s) =>
        defaultKnownCodecs(s)
      // Known codecs for the aliased type.
      case a: Alias if defaultKnownCodecs.contains(a.dealias) =>
        defaultKnownCodecs(a.dealias)
      // Option[X]
      case o if o.isOption =>
        val elementSurface = o.typeArgs(0)
        OptionCodec(factory.ofSurface(elementSurface, seenSet))
      case et: Surface if classOf[Either[_, _]].isAssignableFrom(et.rawType) =>
        EitherCodec(factory.ofSurface(et.typeArgs(0)), factory.ofSurface(et.typeArgs(1)))
      case g: GenericSurface if classOf[Union].isAssignableFrom(g.rawType) =>
        UnionCodec
      // Tuple
      case g: GenericSurface
          if classOf[Product].isAssignableFrom(g.rawType) && g.rawType.getName.startsWith("scala.Tuple") =>
        TupleCodec(g.typeArgs.map(factory.ofSurface(_, seenSet)))
      // Seq[A]
      case g: GenericSurface if classOf[Seq[_]].isAssignableFrom(g.rawType) =>
        val elementSurface = factory.ofSurface(g.typeArgs(0), seenSet)
        g match {
          // IndexedSeq[A]
          case g1: GenericSurface if classOf[IndexedSeq[_]].isAssignableFrom(g1.rawType) =>
            new CollectionCodec.IndexedSeqCodec(g1.typeArgs(0), elementSurface)
          // List[A]
          case g1: GenericSurface if classOf[List[_]].isAssignableFrom(g1.rawType) =>
            new CollectionCodec.ListCodec(g1.typeArgs(0), elementSurface)
          // Generic Seq[A]
          case _ =>
            new CollectionCodec.SeqCodec(g.typeArgs(0), elementSurface)
        }
      // Map[A, B]
      case g: GenericSurface if classOf[Map[_, _]].isAssignableFrom(g.rawType) =>
        CollectionCodec.MapCodec(
          factory.ofSurface(g.typeArgs(0), seenSet),
          factory.ofSurface(g.typeArgs(1), seenSet)
        )
      // Java collections (e.g., ArrayList[A], List[A], Queue[A], Set[A])
      case g: GenericSurface if classOf[java.util.Collection[_]].isAssignableFrom(g.rawType) =>
        val elementSurface = factory.ofSurface(g.typeArgs(0), seenSet)
        CollectionCodec.JavaListCodec(elementSurface)
      // Java Map[A, B]
      case g: GenericSurface if classOf[java.util.Map[_, _]].isAssignableFrom(g.rawType) =>
        CollectionCodec.JavaMapCodec(
          factory.ofSurface(g.typeArgs(0), seenSet),
          factory.ofSurface(g.typeArgs(1), seenSet)
        )
      case es @ EnumSurface(cl, stringExtractor) =>
        new EnumCodec(es)
    }
  }
}
