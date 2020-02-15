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
package wvlet.airframe.benchmark.msgpack

import java.util.concurrent.TimeUnit

import org.msgpack.core.MessageUnpacker
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.msgpack.spi.{MessagePack, Packer, Unpacker}

import scala.util.Random

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class PackBenchmark extends MsgpackData {
  @Benchmark
  def packInt = {
    val packer = MessagePack.newBufferPacker
    intArray.foreach { x => packer.packInt(x) }
  }
}

abstract class UnpackBenchmark extends MsgpackData {
  protected def initUnpacker(byte: Array[Byte]): Unit
  protected def hasNext: Boolean
  protected def unpackInt: Int
  protected def unpackFloat: Float
  protected def unpackBoolean: Boolean

  @Benchmark
  @Group("unpack")
  def intArray(blackhole: Blackhole): Unit = {
    initUnpacker(msgpackIntArray)
    while (hasNext) {
      blackhole.consume(unpackInt)
    }
  }

  @Benchmark
  @Group("unpack")
  def floatArray(blackhole: Blackhole): Unit = {
    initUnpacker(msgpackFloatArray)
    while (hasNext) {
      blackhole.consume(unpackFloat)
    }
  }

  @Benchmark
  @Group("unpack")
  def booleanArray(blackhole: Blackhole): Unit = {
    initUnpacker(msgpackBooleaArray)
    while (hasNext) {
      blackhole.consume(unpackBoolean)
    }
  }
}
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class AirframeMsgpack extends UnpackBenchmark {
  private var unpacker: Unpacker = _
  protected override def initUnpacker(byte: Array[Byte]): Unit = {
    unpacker = MessagePack.newUnpacker(byte)
  }
  protected override def hasNext: Boolean       = unpacker.hasNext
  protected override def unpackInt: Int         = unpacker.unpackInt
  protected override def unpackFloat: Float     = unpacker.unpackFloat
  protected override def unpackBoolean: Boolean = unpacker.unpackBoolean
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class MessagePackJava extends UnpackBenchmark {
  private var unpacker: MessageUnpacker = _
  protected override def initUnpacker(byte: Array[Byte]): Unit = {
    unpacker = org.msgpack.core.MessagePack.newDefaultUnpacker(byte)
  }
  protected override def hasNext: Boolean       = unpacker.hasNext
  protected override def unpackInt: Int         = unpacker.unpackInt()
  protected override def unpackFloat: Float     = unpacker.unpackFloat()
  protected override def unpackBoolean: Boolean = unpacker.unpackBoolean()
}

class MsgpackData {
  private val r = new Random(0) // Use a fixed seed

  private def prepare[A](in: Seq[A], unpack: (Packer, A) => Unit): Array[Byte] = {
    val packer = MessagePack.newBufferPacker
    in.foreach(x => unpack(packer, x))
    packer.toByteArray
  }

  lazy val intArray   = (0 to 1000).map(x => r.nextInt()).toIndexedSeq
  val msgpackIntArray = prepare[Int](intArray, _.packInt(_))

  lazy val floatArray   = (0 to 1000).map(x => r.nextFloat()).toIndexedSeq
  val msgpackFloatArray = prepare[Float](floatArray, _.packFloat(_))

  lazy val booleanArray  = (0 to 1000).map(x => r.nextBoolean()).toIndexedSeq
  val msgpackBooleaArray = prepare[Boolean](booleanArray, _.packBoolean(_))
}
