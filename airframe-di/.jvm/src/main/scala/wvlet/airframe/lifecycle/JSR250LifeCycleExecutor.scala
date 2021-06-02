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
package wvlet.airframe.lifecycle

import java.{lang => jl}

import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.reflect.ClassTag

class MethodCallHook(val injectee: Injectee, method: jl.reflect.Method) extends LifeCycleHook {
  override def toString: String = s"MethodCallHook for [${surface}]"
  override def execute: Unit = {
    method.invoke(injectee.injectee)
  }
}

/**
  * Support @PreDestroy and @PostConstruct
  */
object JSR250LifeCycleExecutor extends LifeCycleEventHandler with LogSupport {
  private def findAnnotation[T <: jl.annotation.Annotation: ClassTag](
      annot: Array[jl.annotation.Annotation]
  ): Option[T] = {
    val c = implicitly[ClassTag[T]]
    annot
      .collectFirst {
        case x if (c.runtimeClass isAssignableFrom x.annotationType) => x
      }
      .asInstanceOf[Option[T]]
  }

  override def onInit(lifeCycleManager: LifeCycleManager, t: Surface, injectee: AnyRef): Unit = {
    for (m <- t.rawType.getMethods; a <- findAnnotation[PostConstruct](m.getDeclaredAnnotations)) {
      trace(s"[${t}] Found @PostConstruct annotation")
      lifeCycleManager.addInitHook(new MethodCallHook(new Injectee(t, injectee), m))
    }

    for (m <- t.rawType.getMethods; a <- findAnnotation[PreDestroy](m.getDeclaredAnnotations)) {
      trace(s"[${t}] Found @PreDestroy annotation")
      lifeCycleManager.addShutdownHook(new MethodCallHook(new Injectee(t, injectee), m))
    }
  }
}
