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

object JMXMBean extends JMXMBeanCompat with LogSupport {
  private case class JMXMethod(m: MethodSurface, jmxAnnotation: JMX)

  def of[A](obj: A, surface: Surface, methodSurfaces: Seq[MethodSurface]): JMXMBean = {
    // Find JMX description
    val cl = obj.getClass

    val description = cl.getAnnotation(classOf[aj.JMX]) match {
      case a if a != null => a.description()
      case _              => ""
    }
    debug(s"Find JMX methods in ${cl}, description:${description}")

    // Collect JMX parameters from the class
    val mbeanParams = collectMBeanParameters(None, surface, methodSurfaces)
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

  private def getJMXAnnotationInfo(h: ParameterBase): (String, String) = {
    (h match {
      case p: Parameter     => p.findAnnotationOf[aj.JMX]
      case m: MethodSurface => m.findAnnotationOf[aj.JMX]
    }).map { a =>
      (a.name, a.description())
    }.getOrElse("", "")
  }

  private def collectMBeanParameters(
      parent: Option[ParameterBase],
      surface: Surface,
      methods: Seq[MethodSurface]
  ): Seq[MBeanParameter] = {
    val jmxParams: Seq[ParameterBase] = surface.params.filter(_.findAnnotationOf[aj.JMX].isDefined) ++ methods.filter(
      _.findAnnotationOf[aj.JMX].isDefined
    )

    jmxParams.flatMap { p =>
      val (name, description) = getJMXAnnotationInfo(p)
      val attrName            = if (name.isEmpty) p.name else name
      val paramName           = parent.map(x => s"${x.name}.${attrName}").getOrElse(attrName)

      if (isNestedMBean(p)) {
        collectMBeanParameters(
          Some(p),
          p.surface,
          // In Scala 3, we cannot traverse nested parameters at ease without unless runtime reflection is available
          // So when using nested JMX, we can support only nested JMX parameters, but not nested methods.
          Seq.empty
        )
      } else {
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

    // We can support only nested parameters
    p.surface.params.exists(x => x.findAnnotationOf[aj.JMX].isDefined)
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
