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
package wvlet.airframe.surface.reflect

import wvlet.log.LogSupport
import java.lang.{reflect => jr}

import wvlet.airframe.surface.{ArraySurface, Surface}

import scala.collection.mutable
import scala.language.existentials

/**
  */
object ReflectTypeUtil extends LogSupport {
  @inline def cls[A](obj: A): Class[_] = {
    if (obj == null) {
      classOf[AnyRef]
    } else {
      obj.asInstanceOf[AnyRef].getClass
    }
  }

  def companionObject[A](cl: Class[A]): Option[Any] = {
    try {
      import scala.language.existentials

      val clName = cl.getName
      val companionCls = if (clName.endsWith("$")) {
        cl
      } else {
        Class.forName(clName + "$")
      }
      val module       = companionCls.getField("MODULE$")
      val companionObj = module.get(null)
      Some(companionObj)
    } catch {
      case e: Throwable => {
        trace(e)
        None
      }
    }
  }

  private[reflect] def access[A <: jr.AccessibleObject, B](f: A, obj: Any)(body: => B): B = {
    synchronized {
      // Use isAccessible() for JDK8 compatibility for a while.
      // For JDK9 or later, we need to use f.canAccess(obj)
      val accessible = f.isAccessible
      try {
        if (!accessible) {
          f.setAccessible(true)
        }
        body
      } finally {
        if (!accessible) {
          f.setAccessible(false)
        }
      }
    }
  }

  def readField(obj: Any, f: jr.Field): Any = {
    access(f, obj) {
      f.get(obj)
    }
  }

  def canBuildFromBuffer(s: Surface): Boolean =
    isArray(s) || isSeq(s.rawType) || isMap(s.rawType) || isSet(s.rawType)
  def canBuildFromString(s: Surface): Boolean =
    isPrimitive(s) || hasStringUnapplyConstructor(s)

  def isPrimitive(s: Surface): Boolean = s.isPrimitive
  def isArray(s: Surface): Boolean     = s.isInstanceOf[ArraySurface]
  def isArrayCls[T](cl: Class[T]): Boolean = {
    cl.isArray || cl.getSimpleName == "Array"
  }

  def isJavaColleciton[T](cl: Class[T]): Boolean = {
    classOf[java.util.Collection[_]].isAssignableFrom(cl)
  }

  /**
    * If the class has unapply(s:String) : T method in the companion object for instantiating class T, returns true.
    *
    * @param s
    * @return
    */
  def hasStringUnapplyConstructor(s: Surface): Boolean = {
    hasStringUnapplyConstructor(s.rawType)
  }

  def hasStringUnapplyConstructor(cl: Class[_]): Boolean = {
    companionObject(cl)
      .map { co =>
        cls(co).getDeclaredMethods.find { p =>
          def acceptString = {
            val t = p.getParameterTypes
            t.length == 1 && t(0) == classOf[String]
          }
          def returnOptionOfT = {
            val rt = p.getGenericReturnType
            val t  = getTypeParameters(rt)
            isOptionCls(p.getReturnType) && t.length == 1 && t(0) == cl
          }
          p.getName == "unapply" && acceptString && returnOptionOfT
        }.isDefined
      }
      .getOrElse(false)
  }

  def isOption(s: Surface): Boolean = s.isOption
  def isOptionCls[T](cl: Class[T]): Boolean = {
    val name = cl.getSimpleName
    // Option None is an object ($)
    name == "Option" || name == "Some" || name == "None$"
  }

  def isBuffer[T](cl: Class[T]): Boolean = {
    classOf[mutable.Buffer[_]].isAssignableFrom(cl)
  }

  def isSeq[T](cl: Class[T]): Boolean = {
    classOf[Seq[_]].isAssignableFrom(cl)
  }

//  def isParSeq[T](cl: Class[T]): Boolean = {
//    // This doesn't work in Scala 2.13
//    classOf[ParSeq[_]].isAssignableFrom(cl)
//  }

  def isIndexedSeq[T](cl: Class[T]): Boolean = {
    classOf[IndexedSeq[_]].isAssignableFrom(cl) || isArrayCls(cl)
  }

  def isMap[T](cl: Class[T]): Boolean = {
    classOf[Map[_, _]].isAssignableFrom(cl)
  }

  def isJavaMap[T](cl: Class[T]): Boolean = {
    classOf[java.util.Map[_, _]].isAssignableFrom(cl)
  }

  def isSet[T](cl: Class[T]): Boolean = {
    classOf[Set[_]].isAssignableFrom(cl)
  }

  def isTuple[T](cl: Class[T]): Boolean = {
    classOf[Product].isAssignableFrom(cl) && cl.getName.startsWith("scala.Tuple")
  }

  def isList[T](cl: Class[T]): Boolean = {
    classOf[List[_]].isAssignableFrom(cl)
  }

  def isEither[T](cl: Class[T]): Boolean = {
    classOf[Either[_, _]].isAssignableFrom(cl)
  }

  /**
    * Get type parameters of the field
    *
    * @param f
    * @return
    */
  def getTypeParameters(f: jr.Field): Array[Class[_]] = {
    getTypeParameters(f.getGenericType)
  }

  /**
    * @param gt
    * @return
    */
  def getTypeParameters(gt: jr.Type): Array[Class[_]] = {
    gt match {
      case p: jr.ParameterizedType => {
        p.getActualTypeArguments.map(resolveClassType(_)).toArray
      }
    }
  }

  def resolveClassType(t: jr.Type): Class[_] = {
    t match {
      case p: jr.ParameterizedType => p.getRawType.asInstanceOf[Class[_]]
      case c: Class[_]             => c
      case _                       => classOf[Any]
    }
  }
}
