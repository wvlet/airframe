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

import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}

trait HttpMessageBase[Raw] { self: HttpMessage[Raw] =>
  inline def withJsonOf[A](a: A): Raw = {
    self.withJson(MessageCodec.of[A].toJson(a))
  }
  inline def withJsonOf[A](a: A, codecFactory: MessageCodecFactory): Raw = {
    self.withJson(codecFactory.of[A].toJson(a))
  }
  inline def withMsgPackOf[A](a: A): Raw = {
    self.withMsgPack(MessageCodec.of[A].toMsgPack(a))
  }
  inline def withMsgPackOf[A](a: A, codecFactory: MessageCodecFactory): Raw = {
    self.withMsgPack(codecFactory.of[A].toMsgPack(a))
  }

  /**
   * Set the content body using a given object. Encoding can be JSON or MsgPack based on Content-Type header.
   */
  inline def withContentOf[A](a: A): Raw = {
    if(self.isContentTypeMsgPack) {
      self.withMsgPack(MessageCodec.of[A].toMsgPack(a))
    }
    else {
      self.withJson(MessageCodec.of[A].toJson(a))
    }
  }

  /**
   * Set the content body using a given object and codec factory. Encoding can be JSON or MsgPack based on Content-Type header.
   */
  inline def withContentOf[A](a: A, codecFactory: MessageCodecFactory): Raw = {
    if(self.isContentTypeMsgPack) {
      self.withMsgPack(codecFactory.of[A].toMsgPack(a))
    }
    else {
      self.withJson(codecFactory.of[A].toJson(a))
    }
  }
}
