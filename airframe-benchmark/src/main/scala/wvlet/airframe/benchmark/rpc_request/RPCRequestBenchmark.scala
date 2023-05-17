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
package wvlet.airframe.benchmark.rpc_request

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.benchmark.http.Greeter
import wvlet.airframe.benchmark.http.Greeter.GreeterResponse
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.client.{HttpChannel, HttpChannelConfig, HttpClients, SyncClientImpl}
import wvlet.airframe.http.netty.NettyRequestHandler
import wvlet.airframe.http._
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class RPCRequestBenchmark extends LogSupport {
  private val emptyServer = new Greeter

  private val noNetworkRPCClient = new SyncClientImpl(
    new HttpChannel {
      private val responseCodec = MessageCodec.of[GreeterResponse]

      override def send(req: HttpMessage.Request, channelConfig: HttpChannelConfig): HttpMessage.Response = {
        val ret = emptyServer.hello(req.message.toContentString)
        Http.response(HttpStatus.Ok_200).withJson(responseCodec.toJson(ret))
      }
      override def sendAsync(req: HttpMessage.Request, channelConfig: HttpChannelConfig): Rx[HttpMessage.Response] = ???
      override def close(): Unit                                                                                   = {}
    },
    Http.client.noLogging.withClientFilter(RxHttpFilter.identity)
  )

  @Setup
  def setup: Unit = {}

  @TearDown
  def teardown: Unit = {}

  @Benchmark
  def rpcBodyOnly(blackhole: Blackhole): Unit = {
    blackhole.consume {
      emptyServer.hello("RPC")
    }
  }

  @Benchmark
  def rpcNettyResponseBuilder(blackhole: Blackhole): Unit = {
    blackhole.consume {
      val resp = Http.response(HttpStatus.Ok_200, """{"message":"Hello, RPC"}""")
      NettyRequestHandler.toNettyResponse(resp)
    }
  }

  @Benchmark
  def rpcNettyResponseBuilderImmutable(blackhole: Blackhole): Unit = {
    blackhole.consume {
      val resp = Http.response(HttpStatus.Ok_200).withJson("""{"message":"Hello, RPC"}""")
      NettyRequestHandler.toNettyResponse(resp)
    }
  }

  private val rpcMethod = RPCMethod(
    path = "/wvlet.airframe.benchmark.http.Greeter/hello",
    rpcInterfaceName = "Greeter",
    methodName = "hello",
    requestSurface = Surface.of[Map[String, Any]],
    responseSurface = Surface.of[String]
  )
  @Benchmark
  def rpcRequestWithRPCMethod(blackhole: Blackhole): Unit = {
    blackhole.consume {
      noNetworkRPCClient.rpc[Map[String, Any], String](
        rpcMethod,
        Map("name" -> "RPC")
      )
    }
  }
}
