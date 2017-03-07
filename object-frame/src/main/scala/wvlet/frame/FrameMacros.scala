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
import scala.reflect.macros.{blackbox => sm}

/**
  *
  */
object FrameMacros {

  class Helper[C <: sm.Context](val c: C) {
    import c.universe._
    val seen = scala.collection.mutable.Set[Type]()
    val memo = scala.collection.mutable.Map[Type, c.Tree]()

    type TypeMatcher = PartialFunction[c.Type, c.Tree]

    private def typeArgsOf(t:c.Type) : List[c.Type] = t match {
      case TypeRef(prefix, symbol, args) =>
        args
      case other =>
        List.empty
    }

    private def typeNameOf(t:c.Type) : String = {
      t.dealias.typeSymbol.fullName
    }

    val toPrimitive : TypeMatcher = {
      case t if t =:= typeOf[Short] => q"wvlet.frame.Primitive.Short"
      case t if t =:= typeOf[Boolean] => q"wvlet.frame.Primitive.Boolean"
      case t if t =:= typeOf[Byte] => q"wvlet.frame.Primitive.Byte"
      case t if t =:= typeOf[Char] => q"wvlet.frame.Primitive.Char"
      case t if t =:= typeOf[Int] => q"wvlet.frame.Primitive.Int"
      case t if t =:= typeOf[Float] => q"wvlet.frame.Primitive.Float"
      case t if t =:= typeOf[Long] => q"wvlet.frame.Primitive.Long"
      case t if t =:= typeOf[Double] => q"wvlet.frame.Primitive.Double"
      case t if t =:= typeOf[String] => q"wvlet.frame.Primitive.String"
    }

    private val toCollection : TypeMatcher = {
      case t if typeNameOf(t) == "scala.collection.Seq" =>
        val elementType = typeArgsOf(t).map(x => toFrame(x)).head
        q"SeqFrame(classOf[$t], ${elementType})"
      case t if typeNameOf(t) == "scala.collection.immutable.Map" =>
        val paramType = typeArgsOf(t).map(x => toFrame(x))
        q"MapFrame(classOf[$t], ${paramType(0)}, ${paramType(1)})"
    }

    private val toAlias : TypeMatcher = {
      case alias @ TypeRef(prefix, symbol, args)
        if symbol.isType && symbol.asType.isAliasType =>
        val inner = toFrame(alias.dealias)
        val name = symbol.asType.name.decodedName.toString
        val fullName = s"${prefix.typeSymbol.fullName}.${name}"
        q"FrameAlias(${name}, ${fullName}, $inner)"
    }

    private val toGeneric : TypeMatcher = {
      case t @ TypeRef(prefix, symbol, args) =>
        val symbolname = t.dealias.typeSymbol.fullName
        //println(s"symbol name: ${symbolname}")
        symbolname match {
          case _ =>
            t.members.find(x => x.isMethod && x.asMethod.isPrimaryConstructor) match {
              case None =>
                println(s"No primary constructor is found for ${t}")
                q"new wvlet.frame.ObjectFrame(classOf[$t])"
              case Some(primaryConstructor) =>
                val classTypeParams = t.typeSymbol.asClass.typeParams
                val params = primaryConstructor.asMethod.paramLists.flatten
                val concreteArgTypes = params.map(_.typeSignature.substituteTypes(classTypeParams, args))
                val frameParams = for ((p, t) <- params.zip(concreteArgTypes)) yield {
                  val name = Literal(Constant(p.name.decodedName.toString))
                  val frame = toFrame(t)
                  val expr = q"wvlet.frame.Param($name, ${frame})"
                  //println(s"p: ${showRaw(expr)}")
                  //println(s"t: ${showRaw(t)}")
                  expr
                }
                q"""new wvlet.frame.Frame {
                       def cl : Class[$t] = classOf[$t]
                       override def params = Seq(..$frameParams)
                    }"""
            }
          // TODO Array types
          // TODO complex types
        }
    }

    private val toGenericFrame : TypeMatcher = {
      case t =>
        q"new wvlet.frame.ObjectFrame(classOf[$t])"
    }

    private val matchers : TypeMatcher =
      toPrimitive orElse
        toCollection orElse
        toAlias orElse
        toGeneric orElse
        toGenericFrame

    def toFrame(t:c.Type) : c.Tree = {
      if(seen.contains(t)) {
        if(memo.contains(t)) {
          memo(t)
        }
        else {
          c.abort(c.enclosingPosition, s"recursive type: ${t.typeSymbol.fullName}")
        }
      }
      else {
        seen += t
        println(s"fullName: ${t.dealias.typeSymbol.fullName}")
        val frame = matchers(t)
        memo += (t -> frame)
        frame
      }
    }

    def extractFullName(typeEv:c.Type) : String = {
      typeEv match {
        case TypeRef(prefix, typeSymbol, args) =>
          typeSymbol.fullName
        case other =>
          typeEv.typeSymbol.fullName
      }
    }

    def genFrame(typeEv:c.Type) : c.Tree = {
      //println(s"genFrame: ${showRaw(typeEv)}")
      val frameGen = typeEv match {
        case TypeRef(prefix, typeSymbol, args) =>
          toFrame(typeEv)
          // TODO Use t.dealias for aliased type
        case other =>
          q"""new wvlet.frame.Frame { def cl : Class[$typeEv] = classOf[$typeEv] }"""
      }
      val fullName = extractFullName(typeEv)
      //println(s"frameGen: ${show(frameGen)}")
      q"wvlet.frame.Frame.frameCache.getOrElseUpdate(${fullName}, ${frameGen})"
    }
  }

  def of[A:c.WeakTypeTag](c: sm.Context) : c.Tree = {
    import c.universe._
    val typeEv = implicitly[c.WeakTypeTag[A]].tpe
    new Helper[c.type](c).genFrame(typeEv)
  }
}
