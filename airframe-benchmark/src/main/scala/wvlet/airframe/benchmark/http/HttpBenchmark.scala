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
package wvlet.airframe.benchmark.http

object HttpBenchmark {
  final val asyncIteration = 100
}

//@State(Scope.Benchmark)
//@BenchmarkMode(Array(Mode.Throughput))
//@OutputTimeUnit(TimeUnit.SECONDS)
//class ScalaPB extends LogSupport {
//
//  private val port                        = IOUtil.randomPort
//  private var client: GreeterBlockingStub = null
//  private var asyncClient: GreeterStub    = null
//  private val executionContext            = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
//  private val server =
//    NettyServerBuilder.forPort(port).addService(GreeterGrpc.bindService(new ScalaPBGreeter(), executionContext)).build()
//  private val channel = ManagedChannelBuilder.forTarget(s"localhost:${port}").usePlaintext().build()
//
//  @Setup
//  def setup: Unit = {
//    server.start
//    client = new GreeterBlockingStub(channel)
//    asyncClient = new GreeterStub(channel)
//  }
//
//  @TearDown
//  def teardown: Unit = {
//    server.shutdown
//    channel.shutdownNow
//    executionContext.shutdownNow()
//  }
//
//  @Benchmark
//  def rpcSync(blackhole: Blackhole): Unit = {
//    blackhole.consume(client.hello(HelloRequest("RPC")))
//  }
//
//  @Benchmark
//  @OperationsPerInvocation(asyncIteration)
//  def rpcAsync(blackhole: Blackhole): Unit = {
//    val counter = new AtomicInteger(0)
//    val futures = for (i <- 0 until asyncIteration) yield {
//      val f = asyncClient
//        .hello(HelloRequest("RPC"))
//      f.onComplete(x => counter.incrementAndGet())(executionContext)
//      f
//    }
//    while (counter.get() != asyncIteration) {
//      Thread.sleep(0)
//    }
//  }
//}
