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
package wvlet.airframe.jmx

import java.lang.annotation.Annotation
import java.lang.reflect.Method

import javax.management._
import wvlet.log.LogSupport
import wvlet.airframe.surface.reflect._
import wvlet.airframe.surface.{MethodSurface, Parameter, ParameterBase, Surface}

import scala.reflect.runtime.{universe => ru}
import wvlet.airframe.{jmx => aj}

/**
  * Expose object information using DynamicMBean
  */
case class JMXMBean(obj: AnyRef, mBeanInfo: MBeanInfo, attributes: Seq[MBeanParameter]) extends DynamicMBean {
  assert(obj != null)
  private lazy val attributeTable = attributes.map(a => a.name -> a).toMap

  override def getAttributes(attributes: Array[String]): AttributeList = {
    val l = new AttributeList(attributes.length)
    for (a <- attributes) {
      l.add(getAttribute(a))
    }
    l
  }
  override def getAttribute(attribute: String): AnyRef = {
    attributeTable.get(attribute) match {
      case Some(a) => a.get(obj)
      case None =>
        throw new AttributeNotFoundException(s"${attribute} is not found in ${obj.getClass.getName}")
    }
  }
  override def getMBeanInfo: MBeanInfo = mBeanInfo

  override def setAttributes(attributes: AttributeList): AttributeList = {
    val l = new AttributeList(attributes.size())
    import scala.jdk.CollectionConverters._
    for (a <- attributes.asList().asScala) {
      l.add(setAttribute(a))
    }
    l
  }
  override def setAttribute(attribute: Attribute): Unit = {
    throw new AttributeNotFoundException(s"Setter for ${attribute.getName} is not found in ${obj.getClass.getName}")
//    attributeTable.get(attribute.getName) match {
//      case Some(a) => a.set(obj, attribute.getValue)
//      case None =>
//
//    }
  }
  override def invoke(actionName: String, params: Array[AnyRef], signature: Array[String]): AnyRef = {
    throw new UnsupportedOperationException("JMXMBean.invoke is not supported")
  }
}

object JMXMBean extends LogSupport {

  private case class JMXMethod(m: MethodSurface, jmxAnnotation: JMX)

  def of[A: ru.WeakTypeTag](obj: A): JMXMBean = {

    // Find JMX description
    val cl = obj.getClass

    val description = cl.getAnnotation(classOf[aj.JMX]) match {
      case a if a != null => a.description()
      case _              => ""
    }
    debug(s"Find JMX methods in ${cl}, description:${description}")

    // Collect JMX parameters from the class
    val tpe = implicitly[ru.WeakTypeTag[A]].tpe

    val mbeanParams = collectMBeanParameters(None, tpe)
    val attrInfo = mbeanParams.map { x =>
      val desc = new ImmutableDescriptor()
      new MBeanAttributeInfo(
        x.name,
        x.valueType.rawType.getName,
        x.description,
        true,
        false,
        false
      )
    }

    val mbeanInfo = new MBeanInfo(
      cl.getName,
      description,
      attrInfo.toArray[MBeanAttributeInfo],
      Array.empty[MBeanConstructorInfo],
      Array.empty[MBeanOperationInfo],
      Array.empty[MBeanNotificationInfo]
    )

    new JMXMBean(obj.asInstanceOf[AnyRef], mbeanInfo, mbeanParams)
  }

  private def getDescription(h: ParameterBase): String = {
    h match {
      case p: Parameter     => p.findAnnotationOf[aj.JMX].map(_.description()).getOrElse("")
      case m: MethodSurface => m.findAnnotationOf[aj.JMX].map(_.description()).getOrElse("")
    }
  }

  private def collectMBeanParameters(parent: Option[ParameterBase], tpe: ru.Type): Seq[MBeanParameter] = {
    val surface = ReflectSurfaceFactory.ofType(tpe)
    val methods = ReflectSurfaceFactory.methodsOfType(tpe)

    val jmxParams: Seq[ParameterBase] = surface.params.filter(_.findAnnotationOf[aj.JMX].isDefined) ++ methods.filter(
      _.findAnnotationOf[aj.JMX].isDefined)

    jmxParams.flatMap { p =>
      val paramName = parent.map(x => s"${x.name}.${p.name}").getOrElse(p.name)
      if (isNestedMBean(p)) {
        ReflectSurfaceFactory.findTypeOf(p.surface) match {
          case Some(tpe) => collectMBeanParameters(Some(p), tpe)
          case None      => Seq.empty
        }
      } else {
        val description = getDescription(p)
        Seq(
          parent match {
            case Some(pt) =>
              NestedMBeanParameter(paramName, description, pt, p)
            case None =>
              MBeanObjectParameter(paramName, description, p)
          }
        )
      }
    }

  }

  private def isNestedMBean(p: ParameterBase): Boolean = {
    import wvlet.airframe.surface.reflect._

    val jmxParams = p.surface.params.find(x => x.findAnnotationOf[aj.JMX].isDefined)
    if (jmxParams.isDefined) {
      true
    } else {
      ReflectSurfaceFactory.findTypeOf(p.surface) match {
        case None => false
        case Some(tpe) =>
          val methods    = ReflectSurfaceFactory.methodsOfType(tpe)
          val jmxMethods = methods.find(m => m.findAnnotationOf[aj.JMX].isDefined)
          jmxMethods.isDefined
      }
    }
  }

  def collectUniqueAnnotations(m: Method): Seq[Annotation] = {
    collectUniqueAnnotations(m.getAnnotations)
  }

  def collectUniqueAnnotations(lst: Array[Annotation]): Seq[Annotation] = {
    var seen   = Set.empty[Annotation]
    val result = Seq.newBuilder[Annotation]

    def loop(lst: Array[Annotation]): Unit = {
      for (a <- lst) {
        if (!seen.contains(a)) {
          seen += a
          result += a
          loop(a.annotationType().getAnnotations)
        }
      }
    }
    loop(lst)
    result.result()
  }

//  def buildDescription(a:Annotation) {
//    for(m <- a.annotationType().getMethods.toSeq) {
//      m.getAnnotation(classOf[DescriptorKey]) match {
//        case descriptorKey if descriptorKey != null =>
//          val name = descriptorKey.value()
//          Try(m.invoke(a)).map { v =>
//
//          }
//
//      }
//
//    }
//  }
//

}
