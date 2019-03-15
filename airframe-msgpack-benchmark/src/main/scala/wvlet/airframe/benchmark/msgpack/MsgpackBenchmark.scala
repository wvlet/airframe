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

import org.openjdk.jmh.annotations._
import wvlet.airframe.msgpack.spi.{BufferPacker, MessagePack, Unpacker}

import scala.util.Random

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class MsgpackBenchmark {

  @Benchmark
  def packInt: Array[Byte] = {
    val packer = MessagePack.newBufferPacker
    MsgpackData.intArray.foreach { x =>
      packer.packInt(x)
    }
    packer.toByteArray
  }

  @Benchmark
  def unpackInt: Seq[Int] = {
    val unpacker = MessagePack.newUnpacker(MsgpackData.msgpackIntArray)
    val b        = Seq.newBuilder[Int]
    while (unpacker.hasNext) {
      b += unpacker.unpackInt
    }
    b.result()
  }
}

object MsgpackData {
  val r = new Random(0) // Use a fixed seed

  lazy val intArray = (0 to 100000).map(x => r.nextInt()).toIndexedSeq
  lazy val msgpackIntArray = {
    val packer = MessagePack.newBufferPacker
    intArray.foreach(packer.packInt(_))
    packer.toByteArray
  }
}
