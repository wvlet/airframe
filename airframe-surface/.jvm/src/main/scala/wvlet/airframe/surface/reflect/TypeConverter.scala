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

import java.io.File
import java.text.DateFormat
import java.util.Date

import wvlet.log.LogSupport
import wvlet.airframe.surface.{Primitive, Surface, Zero}

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters._

/**
  */
object TypeConverter extends LogSupport {
  import java.lang.{reflect => jr}

  import ReflectTypeUtil._

  def convert[T](value: T, targetType: Surface): Option[Any] = {
    if (value == null) {
      Some(Zero.zeroOf(targetType))
    } else {
      if (targetType.isPrimitive) {
        convertToPrimitive(value, targetType)
      } else if (targetType.isOption) {
        if (isOptionCls(cls(value))) {
          Option(value)
        } else {
          Option(convert(value, targetType.typeArgs(0)))
        }
      } else if (isArray(targetType) && isArrayCls(cls(value))) {
        Option(value)
      } else {
        val t: Class[_] = targetType.rawType
        val s: Class[_] = cls(value)
        if (t.isAssignableFrom(s)) {
          Some(value)
        } else if (isBuffer(s)) {
          trace(s"convert buffer $value into $targetType")
          val buf              = value.asInstanceOf[mutable.Buffer[_]]
          val gt: Seq[Surface] = targetType.typeArgs
          val e                = gt(0).rawType
          if (isArray(targetType)) {
            val arr = ClassTag(e).newArray(buf.length).asInstanceOf[Array[Any]]
            buf.copyToArray(arr)
            Some(arr)
          } else if (isList(t)) {
            Some(buf.toList)
          } else if (isIndexedSeq(t)) {
            Some(buf.toIndexedSeq)
          } else if (isSeq(t)) {
            Some(buf.toSeq)
          } else if (isSet(t)) {
            Some(buf.toSet)
          } else if (isMap(t)) {
            Some(buf.asInstanceOf[mutable.Buffer[(_, _)]].toMap)
          } else {
            warn(s"cannot convert ${s.getSimpleName} to ${t.getSimpleName}")
            None
          }
        } else {
          convertToCls(value, targetType.rawType)
        }
      }
    }
  }

  /**
    * Convert the input value into the target type
    */
  def convertToCls[A](value: Any, targetType: Class[A]): Option[A] = {
    val cl: Class[_] = cls(value)
    if (targetType.isAssignableFrom(cl)) {
      Some(value.asInstanceOf[A])
    } else if (hasStringUnapplyConstructor(targetType)) {
      // call unapply
      companionObject(targetType) flatMap { co =>
        val m = cls(co).getDeclaredMethod("unapply", Array(classOf[String]): _*)
        val v = m.invoke(co, Array(value.toString): _*).asInstanceOf[Option[A]]
        v match {
          case Some(c) => v
          case None =>
            debug(s"cannot create an instance of $targetType from $value")
            None
        }
      }
    } else {
      stringConstructor(targetType) match {
        case Some(cc) => Some(cc.newInstance(value.toString).asInstanceOf[A])
        case None     => convertToPrimitive(value, Primitive(targetType))
      }
    }
  }

  /**
    * Convert the input value into the target type
    */
  def convertToPrimitive[A](value: Any, targetType: Surface): Option[A] = {
    if (value == null) {
      None
    } else {
      val s = value.toString
      val v: Any = targetType match {
        case Primitive.String                  => s
        case Primitive.Boolean                 => s.toBoolean
        case Primitive.Int                     => s.toInt
        case Primitive.Float                   => s.toFloat
        case Primitive.Double                  => s.toDouble
        case Primitive.Long                    => s.toLong
        case Primitive.Short                   => s.toShort
        case Primitive.Byte                    => s.toByte
        case Primitive.Char if (s.length == 1) => s(0).toChar
        case t if t.rawType == classOf[File]   => new File(s)
        case t if t.rawType == classOf[Date] =>
          DateFormat.getDateInstance().parse(s)
        case _ =>
          warn(s"""Failed to convert "$s" to ${targetType.toString}""")
          None
      }
      Some(v.asInstanceOf[A])
    }
  }

  def stringConstructor(cl: Class[_]): Option[jr.Constructor[_]] = {
    val cc = cl.getDeclaredConstructors
    cc.find { cc =>
      val pt = cc.getParameterTypes
      pt.length == 1 && pt(0) == classOf[String]
    }
  }
}
