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

import io.grpc.Channel
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.Session
import wvlet.airframe.http.finagle.{Finagle, FinagleSyncClient}
import wvlet.airframe.http.grpc.gRPC
import wvlet.log.LogSupport
import com.twitter.finagle.http.{Request, Response}

/**
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class FinagleBenchmark extends LogSupport {

  private val design                                       = Finagle.server.withRouter(Greeter.router).designWithSyncClient.withProductionMode
  private var session: Option[Session]                     = None
  private var client: ServiceSyncClient[Request, Response] = null

  @Setup
  def setup: Unit = {
    val s = design.newSession
    s.start
    session = Some(s)
    client = new ServiceSyncClient(s.build[FinagleSyncClient])
  }

  @TearDown
  def teardown: Unit = {
    session.foreach(_.shutdown)
  }

  @Benchmark
  def rpc(blackhole: Blackhole): Unit = {
    blackhole.consume(client.Greeter.hello("RPC"))
  }
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class GrpcBenchmark extends LogSupport {

  private val design =
    gRPC.server.withRouter(Greeter.router).designWithChannel.withProductionMode
  private var session: Option[Session]       = None
  private var client: ServiceGrpc.SyncClient = null

  @Setup
  def setup: Unit = {
    val s = design.newSession
    s.start
    val channel = s.build[Channel]
    session = Some(s)
    client = ServiceGrpc.newSyncClient(channel)
  }

  @TearDown
  def teardown: Unit = {
    session.foreach(_.shutdown)
  }

  @Benchmark
  def rpc(blackhole: Blackhole): Unit = {
    blackhole.consume(client.Greeter.hello("RPC"))
  }
}
