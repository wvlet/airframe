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
import com.google.common.util.concurrent.{FutureCallback, Futures}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.grpc.{Channel, ManagedChannelBuilder}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import wvlet.airframe.Session
import wvlet.airframe.benchmark.http.protojava.ProtoJavaGreeter
import wvlet.airframe.http.finagle.{Finagle, FinagleClient, FinagleServer, FinagleSyncClient}
import wvlet.airframe.http.grpc.gRPC
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, TimeUnit}

object HttpBenchmark {
  final val asyncIteration = 100
}

import wvlet.airframe.benchmark.http.HttpBenchmark._

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
    client.close()
    asyncClient.close()
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
      Thread.sleep(0)
    }
  }
}

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
      Thread.sleep(0)
    }
  }
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

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class GrpcJava extends LogSupport {

  private val port                                                       = IOUtil.randomPort
  private var client: protojava.GreeterGrpc.GreeterBlockingStub          = null
  private var asyncClient: protojava.GreeterGrpc.GreeterStub             = null
  private val executor                                                   = Executors.newCachedThreadPool()
  private var futureAsyncClient: protojava.GreeterGrpc.GreeterFutureStub = null
  private val server =
    NettyServerBuilder.forPort(port).addService(new ProtoJavaGreeter).build()
  private val channel = ManagedChannelBuilder.forTarget(s"localhost:${port}").usePlaintext().build()

  @Setup
  def setup: Unit = {
    server.start
    client = protojava.GreeterGrpc.newBlockingStub(channel)
    asyncClient = protojava.GreeterGrpc.newStub(channel)
    futureAsyncClient = protojava.GreeterGrpc.newFutureStub(channel)
  }

  @TearDown
  def teardown: Unit = {
    server.shutdown
    channel.shutdownNow
    executor.shutdownNow()
  }

  @Benchmark
  def rpcSync(blackhole: Blackhole): Unit = {
    blackhole.consume(client.sayHello(protojava.HelloRequest.newBuilder().setName("RPC").build()))
  }

  // @Benchmark
  @OperationsPerInvocation(asyncIteration)
  def rpcFutureAsync(blackhole: Blackhole): Unit = {
    val counter = new AtomicInteger(0)
    for (i <- 0 until asyncIteration) yield {
      val future = futureAsyncClient.sayHello(protojava.HelloRequest.newBuilder().setName("RPC").build())
      Futures.addCallback(
        future,
        new FutureCallback[protojava.HelloReply] {
          override def onSuccess(result: protojava.HelloReply): Unit = {
            blackhole.consume(result.getMessage)
            counter.incrementAndGet()
          }
          override def onFailure(t: Throwable): Unit = ???
        },
        executor
      )
    }
    while (counter.get() != asyncIteration) {
      Thread.sleep(0)
    }
  }

  @Benchmark
  @OperationsPerInvocation(asyncIteration)
  def rpcAsync(blackhole: Blackhole): Unit = {
    val counter = new AtomicInteger(0)
    for (i <- 0 until asyncIteration) yield {
      asyncClient.sayHello(
        protojava.HelloRequest.newBuilder().setName("RPC").build(),
        new StreamObserver[protojava.HelloReply] {
          override def onNext(v: protojava.HelloReply): Unit = {
            blackhole.consume(v.getMessage)
          }
          override def onError(t: Throwable): Unit = {}
          override def onCompleted(): Unit = {
            counter.incrementAndGet()
          }
        }
      )
    }
    while (counter.get() != asyncIteration) {
      Thread.sleep(0)
    }
  }
}
