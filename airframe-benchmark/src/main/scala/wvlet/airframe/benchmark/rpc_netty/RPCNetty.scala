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
package wvlet.airframe.benchmark.rpc_netty

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.Session
import wvlet.airframe.benchmark.http.HttpBenchmark.asyncIteration
import wvlet.airframe.benchmark.http.{Greeter, NewServiceAsyncClient, NewServiceSyncClient}
import wvlet.airframe.http.*
import wvlet.airframe.http.client.{AsyncClient, SyncClient}
import wvlet.airframe.http.netty.Netty
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
      .noLogging
      .design
      .bind[SyncClient].toProvider { (server: HttpServer) =>
        Http.client.noLogging.noClientFilter.newSyncClient(server.localAddress)
      }
      .bind[AsyncClient].toProvider { (server: HttpServer) =>
        Http.client.noLogging.noClientFilter.newAsyncClient(server.localAddress)
      }
      .withProductionMode

  private var session: Option[Session] = None

  private var client: NewServiceSyncClient       = null
  private var asyncClient: NewServiceAsyncClient = null

  private val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  @Setup
  def setup: Unit = {
    val s = design.newSession
    s.start
    session = Some(s)
    client = new NewServiceSyncClient(s.build[SyncClient])
    asyncClient = new NewServiceAsyncClient(s.build[AsyncClient])
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

  @Benchmark
  @OperationsPerInvocation(asyncIteration)
  def rpcAsync(blackhole: Blackhole): Unit = {
    val counter = new AtomicInteger(0)
    for (i <- 0 until asyncIteration) {
      val rx = asyncClient.Greeter
        .hello("RPC").map { x =>
          counter.incrementAndGet()
        }
      blackhole.consume(
        ec.submit {
          new Runnable {
            override def run(): Unit = rx.run(_ => ())
          }
        }
      )
    }
    while (counter.get() != asyncIteration) {
      Thread.sleep(0)
    }
  }
}
