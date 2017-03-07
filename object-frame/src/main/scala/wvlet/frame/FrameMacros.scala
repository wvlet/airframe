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
package wvlet.frame

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.reflect.macros.{blackbox => sm}

/**
  *
  */
object FrameMacros {


  case class ConstructorType[S](
    typeParams:List[S],
    params:List[S]
  )

  def of[A:c.WeakTypeTag](c: sm.Context)(typeEv: c.Tree) : c.Tree = {
    import c.universe._

    val t = typeEv.tpe.typeArgs(0)
    println(s"${showRaw(t)}")

    t match {
      case TypeRef(_, cls, args) =>
        // TODO Use t.dealias for aliased type
        val ct: ConstructorType[Symbol] =
          t.members.find(x => x.isMethod && x.asMethod.isPrimaryConstructor) match {
            case None =>
              println(s"No primary constructor is found for ${t}")
              ConstructorType(List.empty, List.empty)
            case Some(primaryConstructor) =>
              ConstructorType(
                t.typeSymbol.asClass.typeParams,
                primaryConstructor.asMethod.paramLists.flatten
              )
          }
        val resolvedArgs = for (p <- ct.params) yield {
          p.typeSignature.substituteTypes(ct.typeParams, args)
        }
        println(s"params:\n${ct.params.map(showRaw(_)).mkString("\n")}")
        println(s"resolved:\n${resolvedArgs.map(showRaw(_)).mkString("\n")}")
      case other =>
        println(s"${showRaw(other)}")
    }

    q"wvlet.frame.Frame(classOf[$t])"
  }
}
