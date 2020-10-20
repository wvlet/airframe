package greeter.api

import wvlet.airframe.http.RPC
import wvlet.log.LogSupport

@RPC
class GreeterApi extends LogSupport {
  def hello(name: String) = {
    info(s"Received a request from: ${name}")
    s"Hello ${name}!"
  }
}
