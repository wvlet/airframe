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
package example.ui

import example.api.ServiceJSClient
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import wvlet.airframe.rx.Rx
import wvlet.airframe.rx.html.{DOMRenderer, RxElement}
import wvlet.airframe.http.js.JSHttpClient
import wvlet.airframe.http.js.JSHttpClient.MessageEncoding

import scala.scalajs.js.annotation.JSExport
import wvlet.airframe.rx.html.all._
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  */
object ExampleUI extends LogSupport {

  @JSExport
  def main(args: Array[String]): Unit = {
    Logger.setDefaultLogLevel(LogLevel.DEBUG)
    info("Starting UI")

    val main = dom.document.getElementById("main")

    DOMRenderer.renderTo(main, new MainUI)
  }
}

trait RPCService extends LogSupport {
  protected implicit val queue = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  protected val client = new ServiceJSClient()

  def rpc[U](body: ServiceJSClient => Future[U]): Future[U] = {
    val future = body(client)
    future.onComplete {
      case Success(v) =>
      case Failure(e) =>
        warn(e)
    }
    future
  }

}

class MainUI extends RxElement with RPCService {

  private val message = Rx.variable("N/A")

  override def render: RxElement = {
    div(s"Counter: ", Rx.intervalMillis(100))
//
//    div(
//      button(
//        onclick -> { e: MouseEvent =>
//          info(s"Clicked")
//          rpc(_.HelloApi.hello("RPC"))
//            .foreach { resp =>
//              info(s"RPC result: ${resp}")
//              message := resp.message
//            }
//        },
//        "Click Me!"
//      ),
//      message.map { x =>
//        div(s"Message: ${x}")
//      },
//      div(
//        "counter:",
//      )
//    )
  }
}
