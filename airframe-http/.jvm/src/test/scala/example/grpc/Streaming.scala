package example.grpc

import wvlet.airframe.http.rx.RxStream
import wvlet.airframe.http.RPC

@RPC
trait Streaming {
  def serverStreaming(name: String): RxStream[String]
}
