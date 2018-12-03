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
package wvlet.airframe.config
import wvlet.airframe.{DesignOptions, Stage}

/**
  *
  */
case class DesignOptionsWithConfig(override val enabledLifeCycleLogging: Boolean = true,
                                   override val stage: Stage = Stage.DEVELOPMENT,
                                   val config: Config)
    extends DesignOptions(enabledLifeCycleLogging, stage) {

  override def +(other: DesignOptions): DesignOptions = {
    // configs will be overwritten
    other match {
      case o: DesignOptionsWithConfig =>
        new DesignOptionsWithConfig(other.enabledLifeCycleLogging, other.stage, this.config ++ o.config)
      case other =>
        new DesignOptionsWithConfig(other.enabledLifeCycleLogging, other.stage, this.config)
    }
  }

  override def withLifeCycleLogging: DesignOptionsWithConfig = {
    new DesignOptionsWithConfig(enabledLifeCycleLogging = true, stage, config)
  }
  override def noLifecycleLogging: DesignOptionsWithConfig = {
    new DesignOptionsWithConfig(enabledLifeCycleLogging = false, stage, config)
  }

  override def withProductionMode: DesignOptionsWithConfig = {
    new DesignOptionsWithConfig(enabledLifeCycleLogging, Stage.PRODUCTION, config)
  }

  override def withLazyMode: DesignOptionsWithConfig = {
    new DesignOptionsWithConfig(enabledLifeCycleLogging, Stage.DEVELOPMENT, config)
  }

  def withConfig(newConfig: Config): DesignOptions = {
    new DesignOptionsWithConfig(enabledLifeCycleLogging, stage, newConfig)
  }
}
