package example.api

import wvlet.airframe.http.RPC
import wvlet.airframe.rx.RxStream

@RPC
trait GreeterApi {
  def sayHello(message: String): String
  def serverStreaming(message: String): RxStream[String]
  def clientStreaming(message: RxStream[String]): String
  def bidiStreaming(message: RxStream[String]): RxStream[String]
}
