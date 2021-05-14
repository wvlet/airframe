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
package wvlet.airframe.di

import wvlet.airframe.surface.Surface
import wvlet.airframe.di.tracing.{DIStats, Tracer}
import wvlet.log.LogSupport

import wvlet.airframe.di.lifecycle.LifeCycleHookType

/**
  * Design configs
  */
case class DesignOptions(
    enabledLifeCycleLogging: Option[Boolean] = None,
    stage: Option[Stage] = None,
    defaultInstanceInjection: Option[Boolean] = None,
    options: Map[String, Any] = Map.empty
) extends Serializable {

  import DesignOptions._

  def +(other: DesignOptions): DesignOptions = {
    // configs will be overwritten
    new DesignOptions(
      other.enabledLifeCycleLogging.orElse(this.enabledLifeCycleLogging),
      other.stage.orElse(this.stage),
      other.defaultInstanceInjection.orElse(this.defaultInstanceInjection),
      defaultOptionMerger(options, other.options)
    )
  }

  private def defaultOptionMerger(a: Map[String, Any], b: Map[String, Any]): Map[String, Any] = {
    a.foldLeft(b) { (m, keyValue) =>
      val (key, value) = keyValue
      (m.get(key), value) match {
        case (Some(v1: AdditiveDesignOption[_]), v2: AdditiveDesignOption[_]) =>
          m + (key -> v1.addAsDesignOption(v2))
        case _ =>
          m + keyValue
      }
    }
  }

  def withLifeCycleLogging: DesignOptions = {
    this.copy(enabledLifeCycleLogging = Some(true))
  }

  def noLifecycleLogging: DesignOptions = {
    this.copy(enabledLifeCycleLogging = Some(false))
  }

  def withProductionMode: DesignOptions = {
    this.copy(stage = Some(Stage.PRODUCTION))
  }

  def withLazyMode: DesignOptions = {
    this.copy(stage = Some(Stage.DEVELOPMENT))
  }

  def noDefaultInstanceInjection: DesignOptions = {
    this.copy(defaultInstanceInjection = Some(false))
  }

  private[airframe] def withOption[A](key: String, value: A): DesignOptions = {
    this.copy(options = this.options + (key -> value))
  }

  private[airframe] def noOption[A](key: String): DesignOptions = {
    this.copy(options = this.options - key)
  }

  private[airframe] def getOption[A](key: String): Option[A] = {
    options.get(key).map(_.asInstanceOf[A])
  }
}

object DesignOptions {
  private[airframe] trait AdditiveDesignOption[+A] {
    private[airframe] def addAsDesignOption[A1 >: A](other: A1): A1
  }

  private[airframe] def tracerOptionKey = "tracer"
  private[airframe] def statsOptionKey  = "stats"
}

case class LifeCycleHookDesign(lifeCycleHookType: LifeCycleHookType, surface: Surface, hook: Any => Unit) {
  // Override toString to protect calling the hook accidentally
  override def toString: String = s"LifeCycleHookDesign[${lifeCycleHookType}](${surface})"
}
