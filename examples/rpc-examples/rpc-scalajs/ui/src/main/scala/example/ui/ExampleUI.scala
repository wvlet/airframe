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

import example.api.ServiceRPC
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import wvlet.airframe.http._
import wvlet.airframe.rx.{Rx, RxOption}
import wvlet.airframe.rx.html.{DOMRenderer, RxElement}

import scala.scalajs.js.annotation.JSExport
import wvlet.airframe.rx.html._
import wvlet.airframe.rx.html.all._
import wvlet.log.{LogLevel, LogSupport, Logger}

/**
  */
object ExampleUI extends LogSupport {

  @JSExport
  def main(args: Array[String]): Unit = {
    Logger.setDefaultLogLevel(LogLevel.DEBUG)
    info("Starting UI")
    debug("debug log")

    val main = dom.document.getElementById("main")
    DOMRenderer.renderTo(main, new MainUI)
  }
}

class MainUI extends RxElement with LogSupport {
  // Prepare an RPC client
  private val rpcClient = ServiceRPC.newRPCAsyncClient(Http.client.newJSClient)
  private val message = Rx.variable("N/A")

  private def myButton = button(cls -> "btn btn-primary")
  private var counter = 0

  override def render: RxElement = {
    div(
      style -> "padding: 5px",
      myButton(
        onclick -> { (e: MouseEvent) =>
          counter += 1
          debug(s"Sending an RPC request")
          rpcClient.HelloApi
            .hello(s"RPC ${counter}")
            .toRxStream
            .map { resp =>
              message := resp
            }
        },
        "Click: RPC call"
      ),
      message.map { x =>
        div(s"Received an RPC response: ${x}")
      }
    )
  }
}
