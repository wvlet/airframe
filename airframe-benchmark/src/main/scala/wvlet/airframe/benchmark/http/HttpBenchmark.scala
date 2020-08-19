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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import io.grpc.Channel
import io.grpc.stub.StreamObserver
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.Session
import wvlet.airframe.http.finagle.{Finagle, FinagleClient, FinagleServer, FinagleSyncClient}
import wvlet.airframe.http.grpc.gRPC
import wvlet.log.LogSupport

object HttpBenchmark {
  final val asyncIteration = 100
}

import wvlet.airframe.benchmark.http.HttpBenchmark._

/**
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class FinagleBenchmark extends LogSupport {

  private val design =
    Finagle.server
      .withRouter(Greeter.router)
      .designWithSyncClient
      .bind[FinagleClient].toProvider { server: FinagleServer =>
        Finagle.client.newClient(server.localAddress)
      }
      .withProductionMode
  private var session: Option[Session]                              = None
  private var client: ServiceSyncClient[Request, Response]          = null
  private var asyncClient: ServiceClient[Future, Request, Response] = null

  @Setup
  def setup: Unit = {
    val s = design.newSession
    s.start
    session = Some(s)
    client = new ServiceSyncClient(s.build[FinagleSyncClient])
    asyncClient = new ServiceClient(s.build[FinagleClient])
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
    val futures = for (i <- 0 until asyncIteration) yield {
      asyncClient.Greeter.hello("RPC").onSuccess { x =>
        counter.incrementAndGet()
      }
    }
    while (counter.get() != asyncIteration) {
      Thread.`yield`()
    }
  }
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class GrpcBenchmark extends LogSupport {

  private val design =
    gRPC.server.withRouter(Greeter.router).designWithChannel.withProductionMode
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
    }
    while (counter.get() != asyncIteration) {
      Thread.`yield`()
    }
  }

}
