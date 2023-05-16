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
package wvlet.airframe.benchmark.rpc_finagle

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.Session
import wvlet.airframe.benchmark.http.HttpBenchmark.asyncIteration
import wvlet.airframe.benchmark.http.{Greeter, NewServiceSyncClient, ServiceClient, ServiceSyncClient}
import wvlet.airframe.http.Http
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.finagle.{Finagle, FinagleClient, FinagleServer, FinagleSyncClient}
import wvlet.log.LogSupport

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class AirframeFinagle extends LogSupport {

  private val design =
    Finagle.server
      .withRouter(Greeter.router)
      .noLoggingFilter
      .designWithSyncClient
      .bind[FinagleClient].toProvider { (server: FinagleServer) =>
        Finagle.client.newClient(server.localAddress)
      }
      .bind[SyncClient].toProvider { (server: FinagleServer) =>
        Http.client.noLogging.noClientFilter.newSyncClient(server.localAddress)
      }
      .withProductionMode
  private var session: Option[Session]                              = None
  private var client: ServiceSyncClient[Request, Response]          = null
  private var syncClient: NewServiceSyncClient                      = null
  private var asyncClient: ServiceClient[Future, Request, Response] = null

  @Setup
  def setup: Unit = {
    val s = design.newSession
    s.start
    session = Some(s)
    client = new ServiceSyncClient(s.build[FinagleSyncClient])
    syncClient = new NewServiceSyncClient(s.build[SyncClient])
    asyncClient = new ServiceClient(s.build[FinagleClient])
  }

  @TearDown
  def teardown: Unit = {
    session.foreach(_.shutdown)
    client.close()
    syncClient.close()
    asyncClient.close()
  }

  @Benchmark
  def rpcSyncFinagleClient(blackhole: Blackhole): Unit = {
    blackhole.consume(client.Greeter.hello("RPC"))
  }

  @Benchmark
  def rpcSyncDefaultClient(blackhole: Blackhole): Unit = {
    blackhole.consume(syncClient.Greeter.hello("RPC"))
  }

  @Benchmark
  @OperationsPerInvocation(asyncIteration)
  def rpcAsyncFinagleClient(blackhole: Blackhole): Unit = {
    val counter = new AtomicInteger(0)
    val futures = for (i <- 0 until asyncIteration) yield {
      asyncClient.Greeter.hello("RPC").onSuccess { x =>
        counter.incrementAndGet()
      }
    }
    while (counter.get() != asyncIteration) {
      Thread.sleep(0)
    }
  }
}
