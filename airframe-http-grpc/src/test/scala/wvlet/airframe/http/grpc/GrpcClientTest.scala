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
package wvlet.airframe.http.grpc

import io.grpc.stub.StreamObserver
import wvlet.airframe.Design
import wvlet.airframe.http.grpc.example.DemoApiV2
import wvlet.airframe.http.grpc.internal.GrpcRequestHandler
import wvlet.airframe.http.{RPCException, RPCStatus}
import wvlet.airspec.AirSpec
import wvlet.log.{LogSupport, Logger}

import scala.concurrent.Promise

class GrpcClientTest extends AirSpec {

  // TODO Use AirSpec's defaultExecutionContext
  private implicit val sc = scala.concurrent.ExecutionContext.global

  override def design: Design = DemoApiV2.design

  test("GrpcClient") { (client: DemoApiV2.SyncClient) =>
    test("hello") {
      client.hello("v2") shouldBe "Hello v2!"
    }

    test("helloAsync") {
      val p = Promise[String]()
      client.helloAsync(
        "v2 async",
        new StreamObserver[String] with LogSupport {
          override def onNext(value: String): Unit = {
            p.success(value)
          }

          override def onError(t: Throwable): Unit = {
            p.failure(t)
          }

          override def onCompleted(): Unit = {}
        }
      )
      p.future.foreach(value => value shouldBe "Hello v2 async!")
    }

    test("server streaming") {
      val rx = client.serverStreaming("streaming")

      rx.toSeq shouldBe Seq("streaming:0", "streaming:1")
    }

    test("server streaming async") {
      val p = Promise[Seq[String]]()
      client.serverStreamingAsync(
        "async",
        new StreamObserver[String] {
          private val s = Seq.newBuilder[String]
          override def onNext(value: String): Unit = {
            s += value
          }

          override def onError(t: Throwable): Unit = {
            p.failure(t)
          }

          override def onCompleted(): Unit = {
            p.success(s.result())
          }
        }
      )
      p.future.foreach(value => value shouldBe Seq("async:0", "async:1"))
    }

    test("RPCException") {
      Logger.of[GrpcRequestHandler].suppressLogs {
        val ex = intercept[RPCException] {
          client.errorTest("xxx")
        }
        ex.status shouldBe RPCStatus.INVALID_ARGUMENT_U2
        ex.message shouldBe "Hello error: xxx"
      }
    }

    test("RPCException async") {
      val p = Promise[RPCException]()
      client.errorTestAsync(
        "yyy",
        new StreamObserver[String] {
          override def onNext(value: String): Unit = {
            p.failure(new IllegalStateException("Cannot reach here"))
          }

          override def onError(t: Throwable): Unit = {
            t match {
              case e: RPCException =>
                p.success(e)
              case other =>
                p.failure(t)
            }
          }

          override def onCompleted(): Unit = {}
        }
      )

      Logger.of[GrpcRequestHandler].suppressLogAroundFuture {
        p.future.map { (e: RPCException) =>
          e.status shouldBe RPCStatus.INVALID_ARGUMENT_U2
          e.message shouldBe "Hello error: yyy"
        }
      }
    }

  }

}
