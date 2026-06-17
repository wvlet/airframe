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
package wvlet.airframe.http

import wvlet.airframe.http.HttpMessage.Request

/**
  * A per-connection handle for interacting with an established WebSocket connection. It is passed to the callbacks of
  * [[WebSocketHandler]] so that the handler can send outbound frames or close the connection.
  *
  * Implementations (e.g. the Netty backend) are thread-safe: `send` and `close` may be called from any thread.
  */
trait WebSocketContext {

  /**
    * The original HTTP upgrade request that initiated this WebSocket connection. Useful for reading headers, query
    * parameters, or attachments set by upstream filters (e.g. an authenticated user).
    */
  def request: Request

  /**
    * Send a UTF-8 text frame to the client.
    */
  def send(text: String): Unit

  /**
    * Send a binary frame to the client.
    */
  def send(data: Array[Byte]): Unit

  /**
    * Close the connection with a normal closure status (1000).
    */
  def close(): Unit

  /**
    * Close the connection with the given WebSocket close status code and reason.
    */
  def close(statusCode: Int, reason: String): Unit
}

/**
  * A callback interface for handling the lifecycle of a server-side WebSocket connection.
  *
  * All callbacks have no-op defaults, so an implementation only needs to override the events it cares about. A new
  * handler instance is created per connection (see `withWebSocketRoute`), so handlers may hold per-connection mutable
  * state.
  *
  * Note: callbacks are invoked on the server's I/O thread (or the configured handler executor thread pool). Avoid
  * blocking on the I/O thread; sending via [[WebSocketContext]] is thread-safe and may be done from any thread.
  */
trait WebSocketHandler {

  /**
    * Called once after the WebSocket handshake completes and the connection is ready to send/receive frames.
    */
  def onOpen(ctx: WebSocketContext): Unit = {}

  /**
    * Called when a text frame is received from the client.
    */
  def onTextMessage(ctx: WebSocketContext, message: String): Unit = {}

  /**
    * Called when a binary frame is received from the client.
    */
  def onBinaryMessage(ctx: WebSocketContext, message: Array[Byte]): Unit = {}

  /**
    * Called once when the connection is closed, either by the client or the server.
    */
  def onClose(ctx: WebSocketContext): Unit = {}

  /**
    * Called when an error occurs while processing the connection. The connection is typically closed afterward.
    */
  def onError(ctx: WebSocketContext, e: Throwable): Unit = {}
}
