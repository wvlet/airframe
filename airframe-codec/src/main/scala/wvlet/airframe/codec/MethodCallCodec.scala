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
package wvlet.airframe.codec

import org.msgpack.core.MessagePack
import wvlet.surface.MethodSurface

case class MethodCall(methodSurface: MethodSurface, paramArgs: Seq[Any])

object MethodCallBuilder {
  private[codec] val mapCodec = MessageCodec.of[Map[String, String]]

  def of(methodSurface: MethodSurface, codecFactory: MessageCodecFactory = MessageCodec.default): MethodCallBuilder = {
    val argCodec = methodSurface.args.map(x => codecFactory.of(x.surface))
    new MethodCallBuilder(methodSurface, argCodec)
  }
}

/**
  *
  */
class MethodCallBuilder(methodSurface: MethodSurface, argCodec: Seq[MessageCodec[_]]) {

  private val paramListCodec = new ParamListCodec(methodSurface.name, methodSurface.args.toIndexedSeq, argCodec)

  def build(params: Map[String, String]): MethodCall = {
    val msgpack  = MethodCallBuilder.mapCodec.packToBytes(params)
    val unpacker = MessagePack.newDefaultUnpacker(msgpack)
    val v        = new MessageHolder
    val m        = Map.newBuilder[String, Any]
    paramListCodec.unpack(unpacker, v)
    v.getError
      .map(throw _)
      .getOrElse(MethodCall(methodSurface, v.getLastValue.asInstanceOf[Seq[Any]]))
  }
}
