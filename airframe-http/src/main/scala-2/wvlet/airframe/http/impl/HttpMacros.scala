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
package wvlet.airframe.http.impl

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  */
object HttpMacros {

  def rpcSend[RequestType: c.WeakTypeTag, ResponseType: c.WeakTypeTag](
      c: sm.Context
  )(resourcePath: c.Tree, request: c.Tree, requestFilter: c.Tree): c.Tree = {
    import c.universe._
    val t1 = implicitly[c.WeakTypeTag[RequestType]]
    val t2 = implicitly[c.WeakTypeTag[ResponseType]]
    q"""{
          ${c.prefix}.sendRPC(
            ${resourcePath},
            wvlet.airframe.surface.Surface.of[${t1}],
            ${request},
            wvlet.airframe.surface.Surface.of[${t2}],
            ${requestFilter}
          ).asInstanceOf[${t2}]
       }"""
  }

  def rpcSendAsync[RequestType: c.WeakTypeTag, ResponseType: c.WeakTypeTag](
      c: sm.Context
  )(resourcePath: c.Tree, request: c.Tree, requestFilter: c.Tree): c.Tree = {
    import c.universe._
    val t1 = implicitly[c.WeakTypeTag[RequestType]]
    val t2 = implicitly[c.WeakTypeTag[ResponseType]]
    q"""{
          ${c.prefix}.sendRPC(
            ${resourcePath},
            wvlet.airframe.surface.Surface.of[${t1}],
            ${request},
            wvlet.airframe.surface.Surface.of[${t2}],
            ${requestFilter}
          ).asInstanceOf[scala.concurrent.Future[${t2}]]
       }"""
  }

  def toJsonWithCodecFactory[A: c.WeakTypeTag](c: sm.Context)(a: c.Tree, codecFactory: c.Tree): c.Tree = {
    import c.universe._

    val tpe = implicitly[c.WeakTypeTag[A]].tpe
    q"""{
          val codec = ${codecFactory}.of[${tpe}]
          ${c.prefix}.withJson(codec.toJson(${a}))
        }"""
  }

  def toJson[A: c.WeakTypeTag](c: sm.Context)(a: c.Tree): c.Tree = {
    import c.universe._

    val tpe = implicitly[c.WeakTypeTag[A]].tpe
    q"""{
          val codec = wvlet.airframe.codec.MessageCodec.of[${tpe}]
          ${c.prefix}.withJson(codec.toJson(${a}))
        }"""
  }

  def toMsgPackWithCodecFactory[A: c.WeakTypeTag](c: sm.Context)(a: c.Tree, codecFactory: c.Tree): c.Tree = {
    import c.universe._

    val tpe = implicitly[c.WeakTypeTag[A]].tpe
    q"""{
          val codec = ${codecFactory}.of[${tpe}]
          ${c.prefix}.withMsgPack(codec.toMsgPack(${a}))
        }"""
  }

  def toMsgPack[A: c.WeakTypeTag](c: sm.Context)(a: c.Tree): c.Tree = {
    import c.universe._

    val tpe = implicitly[c.WeakTypeTag[A]].tpe
    q"""{
          val codec = wvlet.airframe.codec.MessageCodec.of[${tpe}]
          ${c.prefix}.withMsgPack(codec.toMsgPack(${a}))
        }"""
  }

  def toContentWithCodecFactory[A: c.WeakTypeTag](
      c: sm.Context
  )(a: c.Tree, codecFactory: c.Tree): c.Tree = {
    import c.universe._
    q"""{
        if(${c.prefix}.isContentTypeMsgPack) {
          ${toMsgPackWithCodecFactory[A](c)(a, codecFactory)}
        } else {
          ${toJsonWithCodecFactory[A](c)(a, codecFactory)}
        }
        }"""
  }

  def toContentOf[A: c.WeakTypeTag](c: sm.Context)(a: c.Tree): c.Tree = {
    import c.universe._

    val tpe = implicitly[c.WeakTypeTag[A]].tpe
    q"""{
        if(${c.prefix}.isContentTypeMsgPack) {
          ${toMsgPack[A](c)(a)}
        } else {
          ${toJson[A](c)(a)}
        }
        }"""
  }

}
