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
package wvlet.airframe.tablet.msgpack

import java.util

import org.msgpack.core.{MessagePacker, MessageUnpacker}
import org.msgpack.value.StringValue
import org.msgpack.value.ValueFactory._
import wvlet.surface.Surface

import scala.collection.JavaConverters._

/**
  *
  */
object CollectionCodec {

  case class SeqCodec[A](surface: Surface, elementCodec: MessageCodec[A]) extends MessageCodec[Seq[A]] {
    override def pack(p: MessagePacker, v: Seq[A]): Unit = {
      // elements
      // [e1, e2, ...]
      val len = v.length
      p.packArrayHeader(len)
      for (e <- v) {
        elementCodec.pack(p, e)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
      // Read elements
      val len = u.unpackArrayHeader()
      val b   = Seq.newBuilder[Any]
      b.sizeHint(len)
      for (i <- 0 until len) {
        elementCodec.unpack(u, v)
        if (!v.isNull) {
          b += v.getLastValue
        }
      }
      v.setObject(b.result())
    }
  }

  // TODO Just use SeqCodec for Scala and adapt the result type
  case class JavaListCodec[A](elementCodec: MessageCodec[A]) extends MessageCodec[java.util.List[A]] {
    override def pack(p: MessagePacker, v: util.List[A]): Unit = {
      val len = v.size
      p.packArrayHeader(len)
      for (e <- v.asScala) {
        elementCodec.pack(p, e)
      }
    }

    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
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
    override def pack(p: MessagePacker, m: Map[A, B]): Unit = {
      p.packMapHeader(m.size)
      for ((k, v) <- m.seq) {
        keyCodec.pack(p, k)
        valueCodec.pack(p, v)
      }
    }
    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
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
    override def pack(p: MessagePacker, m: java.util.Map[A, B]): Unit = {
      p.packMapHeader(m.size)
      for ((k, v) <- m.asScala.seq) {
        keyCodec.pack(p, k)
        valueCodec.pack(p, v)
      }
    }
    override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
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
