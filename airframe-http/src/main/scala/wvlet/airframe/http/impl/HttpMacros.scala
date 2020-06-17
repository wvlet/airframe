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

import scala.reflect.macros.{blackbox => sm}

/**
  */
object HttpMacros {
  def newServerException[A: c.WeakTypeTag](c: sm.Context)(request: c.Tree, status: c.Tree, content: c.Tree): c.Tree = {
    import c.universe._
    val tpe = implicitly[c.WeakTypeTag[A]].tpe
    q"""{
          val e = Http.serverException(${status})
          if (${request}.acceptsMsgPack) {
            e.withContentTypeMsgPack
          } else {
            e
          }
          e.withContentOf[${tpe}](${content})
        }"""
  }

  def newServerExceptionWithCodecFactory[A: c.WeakTypeTag](
      c: sm.Context
  )(request: c.Tree, status: c.Tree, content: c.Tree, codecFactory: c.Tree): c.Tree = {
    import c.universe._
    val tpe = implicitly[c.WeakTypeTag[A]].tpe
    q"""{
          val e = Http.serverException(${status})
          if (${request}.acceptsMsgPack) {
            e.withContentTypeMsgPack
          } else {
            e
          }
          e.withContentOf[${tpe}](${content}, ${codecFactory})
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
