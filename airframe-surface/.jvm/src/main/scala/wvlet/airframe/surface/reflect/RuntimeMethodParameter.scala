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

import java.{lang => jl}
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import wvlet.airframe.surface.*
import wvlet.log.LogSupport

import scala.util.Try

/**
  * MethodParameter implementation using reflection for accessing parameter values
  */
case class RuntimeMethodParameter(
    method: MethodRef,
    index: Int,
    name: String,
    surface: Surface
) extends MethodParameter
    with LogSupport {
  override def toString: String = s"${name}:${surface.name}"

  private lazy val field: jl.reflect.Field = Try {
    // Escape quoted parameter names
    // TODO There migfht be other cases. Need a more generic method
    val paramName = name
      .replaceAll("-", "\\$minus")
      .replaceAll("\\+", "\\$plus")
      .replaceAll("=", "\\$eq")
    method.owner.getDeclaredField(paramName)
  }.getOrElse {
    // private fields in case classes can have special field names if default parameters are defined.
    // https://github.com/wvlet/airframe/issues/901
    val privateFieldName = method.owner.getName.replaceAll("\\.", "\\$") + s"$$$$${name}"
    method.owner.getDeclaredField(privateFieldName)
  }

  def get(x: Any): Any = {
    try {
      ReflectTypeUtil.readField(x, field)
    } catch {
      case e: IllegalAccessException =>
        error(s"read field: class ${surface.rawType}, field:${field.getName}", e)
        throw e
    }
  }

  def getDefaultValue: Option[Any] = {
    // This supports a case that method owner is a Trait. In that case, Scala compiler encodes method default parameter
    // into a default method in an Interface, instead of companion object.
    // To fetch the default value, it is required to invoke the special method on an instance of the Trait.
    // So we need to instantiate the Trait, by building a Proxy object that implements the Trait.
    val maybeTraitMethod =
      findInstanceMethod("%s$default$%d".format(this.method.name, index + 1)).filter(_.getDeclaringClass.isInterface)
    val maybeValueFromInstanceMethod = maybeTraitMethod.map { m =>
      val proxyInstance = java.lang.reflect.Proxy.newProxyInstance(
        m.getDeclaringClass.getClassLoader,
        Array(m.getDeclaringClass),
        (proxy: Any, method: Method, args: Array[AnyRef]) => {
          val lookup =
            try {
              val classConstructor =
                classOf[MethodHandles.Lookup].getDeclaredConstructor(classOf[Class[_]], classOf[Int])
              classConstructor.setAccessible(true)
              // Explicit type cast can be dropped if we drop Scala 2.12
              val accessModifiers =
                (MethodHandles.Lookup.PUBLIC | MethodHandles.Lookup.PRIVATE).asInstanceOf[java.lang.Integer]
              classConstructor.newInstance(method.getDeclaringClass, accessModifiers)
            } catch {
              case _: NoSuchMethodException =>
                // In JDK 17+, Lookup's private constructor is gone.
                // An instance returned by lookup() factory works.
                MethodHandles.lookup()
            }
          val methodHandle = lookup.unreflectSpecial(method, method.getDeclaringClass).bindTo(proxy)
          methodHandle.invokeWithArguments(args: _*)
        }
      )
      m.invoke(proxyInstance)
    }

    maybeValueFromInstanceMethod.orElse(ReflectTypeUtil.companionObject(method.owner).flatMap { companion =>
      def findMethod(name: String) = {
        try {
          Some(ReflectTypeUtil.cls(companion).getDeclaredMethod(name))
        } catch {
          case e: NoSuchMethodException => None
        }
      }
      // Find Scala methods for retrieving default values. Since Scala 2.10 appply or $lessinit$greater$ can be the prefix
      val m =
        findMethod("apply$default$%d".format(index + 1))
          .orElse(findMethod("$lessinit$greater$default$%d".format(index + 1)))
      try {
        m.map(_.invoke(companion))
      } catch {
        case e: Throwable =>
          None
      }
    })
  }

  override def getMethodArgDefaultValue(methodOwner: Any): Option[Any] = {
    try {
      val methodName = "%s$default$%d".format(method.name, index + 1)
      val dm         = methodOwner.getClass.getMethod(methodName)
      Some(dm.invoke(methodOwner))
    } catch {
      case e: Throwable =>
        None
    }
  }

  override def isRequired: Boolean = {
    val annots = this.findAnnotationOf[required]
    annots.isDefined
  }

  override def isSecret: Boolean = {
    val annots = this.findAnnotationOf[secret]
    annots.isDefined
  }

  private def findInstanceMethod(name: String) = Try(method.owner.getMethod(name)).toOption
}
