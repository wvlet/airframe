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
package wvlet.airframe.spec
import wvlet.airframe.AirframeMacros

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  *
  */
private[spec] object AirSpecMacros {

  def sourceCode(c: sm.Context): c.Tree = {
    import c.universe._
    c.internal.enclosingOwner
    val pos = c.enclosingPosition
    q"wvlet.airframe.SourceCode(${pos.source.path}, ${pos.source.file.name}, ${pos.line}, ${pos.column})"
  }

  def pendingImpl(c: sm.Context): c.Tree = {
    import c.universe._
    q"""
       throw wvlet.airframe.spec.spi.Pending("pending", ${sourceCode(c)})
     """
  }

  def runImpl[A: c.WeakTypeTag](c: sm.Context): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    q"""{
           ${new AirframeMacros.BindHelper[c.type](c).registerTraitFactory(t)}
           import wvlet.airframe.spec.spi.AirSpecContext._
           val context = ${c.prefix}
           val spec = context.callNewSpec(wvlet.airframe.surface.Surface.of[${t}])
           context.callRunInternal(spec, wvlet.airframe.surface.Surface.methodsOf[${t}])
           spec.asInstanceOf[${t}]
        }
    """
  }

  def runSpecImpl[A: c.WeakTypeTag](c: sm.Context)(spec: c.Tree): c.Tree = {
    import c.universe._
    val t = implicitly[c.WeakTypeTag[A]].tpe
    q"""
        wvlet.airframe.spec.spi.AirSpecContext.AirSpecContextAccess(${c.prefix})
          .callRunInternal(${spec}, wvlet.airframe.surface.Surface.methodsOf[${t}])
          .asInstanceOf[${t}]
    """
  }
}
