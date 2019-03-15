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
@OutputTimeUnit(TimeUnit.SECONDS)
class MsgpackBenchmark {

  @Setup
  def init: Unit = {
    // Initialize data
    val i  = MsgpackData.intArray
    val im = MsgpackData.msgpackIntArray
  }

  @Benchmark
  def packInt: Unit = {
    val packer = MessagePack.newBufferPacker
    MsgpackData.intArray.foreach { x =>
      packer.packInt(x)
    }
  }

  @Benchmark
  def unpackInt: Unit = {
    val unpacker = MessagePack.newUnpacker(MsgpackData.msgpackIntArray)
    while (unpacker.hasNext) {
      unpacker.unpackInt
    }
  }
}

object MsgpackData {
  val r = new Random(0) // Use a fixed seed

  lazy val intArray = (0 to 1000).map(x => r.nextInt()).toIndexedSeq
  lazy val msgpackIntArray = {
    val packer = MessagePack.newBufferPacker
    intArray.foreach(packer.packInt(_))
    packer.toByteArray
  }
}
