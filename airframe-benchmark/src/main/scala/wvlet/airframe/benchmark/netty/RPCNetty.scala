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
package wvlet.airframe.benchmark.netty

import com.twitter.util.Future
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.Session
import wvlet.airframe.benchmark.http.HttpBenchmark.asyncIteration
import wvlet.airframe.benchmark.http.{Greeter, NewServiceSyncClient, ServiceClient}
import wvlet.airframe.http.Http
import wvlet.airframe.http.client.{AsyncClient, SyncClient}
import wvlet.airframe.http.finagle.{Finagle, FinagleClient}
import wvlet.airframe.http.netty.{Netty, NettyServer}
import wvlet.log.LogSupport

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.ExecutionContext

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class AirframeRPCNetty extends LogSupport {

  private val design =
    Netty.server
      .withRouter(Greeter.router)
      // .noLoggingFilter
      .design
      .bind[SyncClient].toProvider { (server: NettyServer) =>
        Http.client.newSyncClient(server.localAddress)
      }
      .bind[AsyncClient].toProvider { (server: NettyServer) =>
        Http.client.newAsyncClient(server.localAddress)
      }
      .bind[FinagleClient].toProvider { (server: NettyServer) =>
        Finagle.client.newClient(server.localAddress)
      }
      .withProductionMode

  private var session: Option[Session] = None

  private var client: NewServiceSyncClient = null
  // private var asyncClient: NewServiceAsyncClient = null
  private var asyncClient: ServiceClient[Future, com.twitter.finagle.http.Request, com.twitter.finagle.http.Response] =
    null

  private val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  @Setup
  def setup: Unit = {
    val s = design.newSession
    s.start
    session = Some(s)
    client = new NewServiceSyncClient(s.build[SyncClient])
    // asyncClient = new NewServiceAsyncClient(s.build[AsyncClient])
    asyncClient = new ServiceClient(s.build[FinagleClient])
  }

  @TearDown
  def teardown: Unit = {
    session.foreach(_.shutdown)
    client.close()
    asyncClient.close()
    ec.shutdownNow()
  }

  @Benchmark
  def rpcSync(blackhole: Blackhole): Unit = {
    blackhole.consume(client.Greeter.hello("RPC"))
  }

  // TODO: AsyncClient with JavaClientChannel can be faster
//  @Benchmark
//  @OperationsPerInvocation(asyncIteration)
//  def rpcAsync(blackhole: Blackhole): Unit = {
//    val counter = new AtomicInteger(0)
//    val futures = for (i <- 0 until asyncIteration) yield {
//      asyncClient.Greeter
//        .hello("RPC").onComplete { x =>
//          counter.incrementAndGet()
//        }(ec)
//    }
//    while (counter.get() != asyncIteration) {
//      Thread.sleep(0)
//    }
//  }

  @Benchmark
  @OperationsPerInvocation(asyncIteration)
  def rpcAsync(blackhole: Blackhole): Unit = {
    val counter = new AtomicInteger(0)
    val futures = for (i <- 0 until asyncIteration) yield {
      asyncClient.Greeter
        .hello("RPC").foreach { x =>
          counter.incrementAndGet()
        }
    }
    while (counter.get() != asyncIteration) {
      Thread.sleep(0)
    }
  }
}
