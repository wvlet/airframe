package example.api

import wvlet.airframe.http.RPC
import wvlet.airframe.rx.Rx

@RPC
trait GreeterApi {
  def sayHello(message: String): String
  def serverStreaming(message: String): Rx[String]
  def clientStreaming(message: Rx[String]): String
  def bidiStreaming(messaage: Rx[String]): Rx[String]
}
