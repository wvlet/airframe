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
package wvlet.surface.reflect

import wvlet.log.LogSupport
import java.lang.{reflect => jr}

/**
  *
  */
object ReflectTypeUtil extends LogSupport {

  @inline def cls[A](obj: A): Class[_] = obj.asInstanceOf[AnyRef].getClass

  def companionObject[A](cl: Class[A]): Option[Any] = {
    try {
      import scala.language.existentials

      val clName = cl.getName
      val companionCls = if (clName.endsWith("$")) {
        cl
      }
      else {
        Class.forName(clName + "$")
      }
      val module = companionCls.getField("MODULE$")
      val companionObj = module.get(null)
      Some(companionObj)
    }
    catch {
      case e: Throwable => {
        trace(e)
        None
      }
    }
  }

  private[reflect] def access[A <: jr.AccessibleObject, B](f: A)(body: => B): B = {
    synchronized {
      val accessible = f.isAccessible
      try {
        if (!accessible) {
          f.setAccessible(true)
        }
        body
      }
      finally {
        if (!accessible) {
          f.setAccessible(false)
        }
      }
    }
  }

  def readField(obj: Any, f: jr.Field): Any = {
    access(f) {
      f.get(obj)
    }
  }
}
