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

import java.util

import wvlet.airframe.codec.MessagePackApi._
import wvlet.surface.{Surface, Zero}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  *
  */
object CollectionCodec {

  object BaseSeqCodec {
    def pack[A](p: Packer, v: Seq[A], elementCodec: MessageCodec[A]): Unit = {
      // elements
      // [e1, e2, ...]
      val len = v.length
      p.packArrayHeader(len)
      for (e <- v) {
        elementCodec.pack(p, e)
      }
    }

    def unpack[A](u: Unpacker, v: MessageHolder, surface: Surface, elementCodec: MessageCodec[A], newBuilder: => mutable.Builder[A, Seq[A]]): Unit = {
      // Read elements
      val len = u.unpackArrayHeader()
      val b   = newBuilder
      b.sizeHint(len)
      for (i <- 0 until len) {
        elementCodec.unpack(u, v)
        if (v.isNull) {
          // Add default value
          b += Zero.zeroOf(surface).asInstanceOf[A]
        } else {
          b += v.getLastValue.asInstanceOf[A]
        }
      }
      v.setObject(b.result())
    }
  }

  case class SeqCodec[A](surface: Surface, elementCodec: MessageCodec[A]) extends MessageCodec[Seq[A]] {
    override def pack(p: Packer, v: Seq[A]): Unit = {
      BaseSeqCodec.pack(p, v, elementCodec)
    }

    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      BaseSeqCodec.unpack(u, v, surface, elementCodec, Seq.newBuilder[A])
    }
  }

  case class IndexedSeqCodec[A](surface: Surface, elementCodec: MessageCodec[A]) extends MessageCodec[IndexedSeq[A]] {
    override def pack(p: Packer, v: IndexedSeq[A]): Unit = {
      BaseSeqCodec.pack(p, v, elementCodec)
    }

    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      BaseSeqCodec.unpack(u, v, surface, elementCodec, IndexedSeq.newBuilder[A])
    }
  }

  // TODO Just use SeqCodec for Scala and adapt the result type
  case class JavaListCodec[A](elementCodec: MessageCodec[A]) extends MessageCodec[java.util.List[A]] {
    override def pack(p: Packer, v: util.List[A]): Unit = {
      val len = v.size
      p.packArrayHeader(len)
      for (e <- v.asScala) {
        elementCodec.pack(p, e)
      }
    }

    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val len = u.unpackArrayHeader()
      val b   = Seq.newBuilder[Any]
      b.sizeHint(len)
      for (i <- 0 until len) {
        elementCodec.unpack(u, v)
        b += v.getLastValue
      }
      v.setObject(b.result().asJava)
    }
  }

  case class MapCodec[A, B](keyCodec: MessageCodec[A], valueCodec: MessageCodec[B]) extends MessageCodec[Map[A, B]] {
    override def pack(p: Packer, m: Map[A, B]): Unit = {
      p.packMapHeader(m.size)
      for ((k, v) <- m.seq) {
        keyCodec.pack(p, k)
        valueCodec.pack(p, v)
      }
    }
    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val len = u.unpackMapHeader()
      val b   = Map.newBuilder[Any, Any]
      b.sizeHint(len)
      for (i <- 0 until len) {
        keyCodec.unpack(u, v)
        val key = v.getLastValue
        valueCodec.unpack(u, v)
        val value = v.getLastValue
        b += (key -> value)
      }
      v.setObject(b.result())
    }
  }

  // TODO Just use MapCodec for Scala and adapt the result type
  case class JavaMapCodec[A, B](keyCodec: MessageCodec[A], valueCodec: MessageCodec[B]) extends MessageCodec[java.util.Map[A, B]] {
    override def pack(p: Packer, m: java.util.Map[A, B]): Unit = {
      p.packMapHeader(m.size)
      for ((k, v) <- m.asScala.seq) {
        keyCodec.pack(p, k)
        valueCodec.pack(p, v)
      }
    }
    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val len = u.unpackMapHeader()
      val b   = Map.newBuilder[Any, Any]
      b.sizeHint(len)
      for (i <- 0 until len) {
        keyCodec.unpack(u, v)
        val key = v.getLastValue
        valueCodec.unpack(u, v)
        val value = v.getLastValue
        b += (key -> value)
      }
      v.setObject(b.result().asJava)
    }
  }

}
