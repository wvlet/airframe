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
package wvlet.surface

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox => sm}

/**
  *
  */
private[surface] class ConstructorMacros[C <: sm.Context, T: C#WeakTypeTag](val c:C) {

  import c.universe._

  lazy val targetType: Type = implicitly[c.WeakTypeTag[T]].tpe

  lazy val publicConstructors: Iterable[Symbol] = {
    targetType.members
    .filter(m => m.isMethod && m.asMethod.isConstructor && m.isPublic)
    .filterNot(isPhantomConstructor)
  }

  lazy val primaryConstructor: Option[Symbol] = publicConstructors.find(_.asMethod.isPrimaryConstructor)

  lazy val constructorParamLists: Option[List[List[Symbol]]] = primaryConstructor.map(_.asMethod.paramLists.filterNot(_.headOption.exists(_.isImplicit)))

//  lazy val constructorArgs: Option[List[List[Tree]]] = {
//    constructorParamLists.map(wireConstructorParams)
//  }
//
//  lazy val constructorTree: Option[Tree] = {
//    val constructionMethodTree: Tree = Select(New(Ident(targetType.dealias.typeSymbol)), termNames.CONSTRUCTOR)
//    constructorArgs.map(_.foldLeft(constructionMethodTree)((acc: Tree, args: List[Tree]) => Apply(acc, args)))
//  }
//

  def isPhantomConstructor(constructor: Symbol): Boolean = constructor.asMethod.fullName.endsWith("$init$")
}
