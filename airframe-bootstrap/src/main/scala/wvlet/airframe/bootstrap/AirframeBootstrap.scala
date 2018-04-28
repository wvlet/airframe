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
package wvlet.airframe.bootstrap

import wvlet.airframe.{Design, Session}
import wvlet.config.Config
import wvlet.log.LogSupport

/**
  *
  */
trait AirframeBootstrap extends LogSupport {
  val config: Config
  val design: Design

  def init: Unit     = {}
  def postMain: Unit = {}

  protected def showConfig: Unit = {
    info("Configurations:")
    for (c <- config.getAll) {
      info(s"${c.tpe}: ${c.value}")
    }
  }

  def main[U](body: Session => U): U = {
    showConfig
    init
    try {
      design.withSession { session =>
        body(session)
      }
    } finally {
      postMain
    }
  }
}
