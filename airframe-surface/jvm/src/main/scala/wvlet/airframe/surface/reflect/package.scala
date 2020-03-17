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

package wvlet.airframe.surface

import java.lang.annotation.Annotation
import java.lang.reflect.Method
import java.{lang => jl}

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  *
  */
package object reflect {
  private[reflect] def findAnnotation[T <: jl.annotation.Annotation: ClassTag](
      annot: Seq[jl.annotation.Annotation]
  ): Option[T] = {
    val c = implicitly[ClassTag[T]]
    annot
      .collectFirst {
        case x if (c.runtimeClass isAssignableFrom x.annotationType) => x
      }
      .asInstanceOf[Option[T]]
  }

  implicit class ToRuntimeSurface(s: Surface) {
    def findAnnotationOf[T <: jl.annotation.Annotation: ClassTag]: Option[T] = {

      def loop(cl: Class[_]): Seq[Annotation] = {
        cl match {
          case null => Seq.empty
          case other =>
            cl.getDeclaredAnnotations ++ cl.getInterfaces.flatMap(loop)
        }
      }

      val annot = loop(s.rawType)
      findAnnotation[T](annot)
    }
  }

  implicit class ToRuntimeSurfaceParameter(p: Parameter) {
    def annotations: Array[Array[jl.annotation.Annotation]] = {
      p match {
        case mp: MethodParameter =>
          Try {
            if (mp.method.name == "<init>") {
              // constructor
              val owner = mp.method.owner
              Try(owner.getDeclaredConstructor(mp.method.paramTypes: _*)).toOption
                .map(_.getParameterAnnotations)
                .getOrElse {
                  // inner classes
                  owner
                    .getDeclaredConstructors()(0)
                    .getParameterAnnotations()
                    .tail
                }
            } else {
              mp.method.owner.getDeclaredMethod(mp.method.name, mp.method.paramTypes: _*).getParameterAnnotations
            }
          }.getOrElse(Array.empty)
        case other =>
          Array.empty
      }
    }

    def findAnnotationOf[T <: jl.annotation.Annotation: ClassTag]: Option[T] = {
      val annots = annotations
      if (p.index < annots.length) {
        findAnnotation[T](annots(p.index))
      } else {
        None
      }
    }
  }

  implicit class ToRuntimeMethodSurface(m: MethodSurface) {
    def annotations: Seq[jl.annotation.Annotation] = {
      Try {
        val cl             = m.owner.rawType
        val methodArgTypes = m.args.map(_.surface.rawType)
        def loop(cl: Class[_]): Seq[Annotation] = {
          cl match {
            case null => Seq.empty
            case other =>
              Try(cl.getMethod(m.name, methodArgTypes: _*)) match {
                case Success(mt) =>
                  mt.getDeclaredAnnotations ++
                    cl.getInterfaces.flatMap(parent => loop(parent))
                case _ =>
                  Seq.empty
              }
          }
        }
        val annot = loop(cl)
        annot
      } match {
        case Success(annot) =>
          annot
        case Failure(e) =>
          Seq.empty
      }
    }

    def findAnnotationOf[T <: jl.annotation.Annotation: ClassTag]: Option[T] = {
      findAnnotation[T](annotations)
    }
  }
}
