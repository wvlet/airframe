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
package wvlet.airframe.http.router

import wvlet.airframe._
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

/**
  */
trait ControllerProvider {
  def findController(session: Session, controllerSurface: Surface): Option[Any]
}

object ControllerProvider {
  object defaultControllerProvider extends ControllerProvider with LogSupport {
    override def findController(session: Session, controllerSurface: Surface): Option[Any] = {
      Try(session.getInstanceOf(controllerSurface)) match {
        case Success(controller) =>
          Some(controller)
        case Failure(e) =>
          warn(e)
          None
      }
    }
  }
}
