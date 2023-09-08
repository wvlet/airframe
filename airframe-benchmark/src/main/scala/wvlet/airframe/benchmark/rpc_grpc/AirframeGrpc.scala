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
package wvlet.airframe.benchmark.rpc_grpc

import io.grpc.Channel
import io.grpc.stub.StreamObserver
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.Session
import wvlet.airframe.benchmark.http.HttpBenchmark.asyncIteration
import wvlet.airframe.benchmark.http.{Greeter, ServiceGrpc}
import wvlet.airframe.http.grpc.gRPC
import wvlet.log.LogSupport

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class AirframeGrpc extends LogSupport {

  private val design =
    gRPC.server
      .withRouter(Greeter.router)
      .noRequestLogging
      .designWithChannel
      .withProductionMode
  private var session: Option[Session]             = None
  private var client: ServiceGrpc.SyncClient       = null
  private var asyncClient: ServiceGrpc.AsyncClient = null

  @Setup
  def setup: Unit = {
    val s = design.newSession
    s.start
    val channel = s.build[Channel]
    session = Some(s)
    client = ServiceGrpc.newSyncClient(channel)
    asyncClient = ServiceGrpc.newAsyncClient(channel)
  }

  @TearDown
  def teardown: Unit = {
    session.foreach(_.shutdown)
  }

  @Benchmark
  def rpcSync(blackhole: Blackhole): Unit = {
    blackhole.consume(client.Greeter.hello("RPC"))
  }

  @Benchmark
  @OperationsPerInvocation(asyncIteration)
  def rpcAsync(blackhole: Blackhole): Unit = {
    val counter = new AtomicInteger(0)
    for (i <- 0 until asyncIteration) {
      blackhole.consume(
        asyncClient.Greeter.hello(
          "RPC",
          new StreamObserver[String] {
            override def onNext(v: String): Unit = {
              blackhole.consume(v)
            }
            override def onError(t: Throwable): Unit = {}
            override def onCompleted(): Unit = {
              counter.incrementAndGet()
            }
          }
        )
      )
    }
    while (counter.get() != asyncIteration) {
      Thread.sleep(0)
    }
  }
}
