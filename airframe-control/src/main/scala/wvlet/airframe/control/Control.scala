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
package wvlet.airframe.control

import scala.util.control.NonFatal

/**
  *
  */
object Control {
  def withResource[R <: AutoCloseable, U](resource: R)(body: R => U): U = {
    try {
      body(resource)
    } finally {
      closeQuietly(resource)
    }
  }

  def withResources[R1 <: AutoCloseable, R2 <: AutoCloseable, U](resource1: R1, resource2: R2)(
      body: (R1, R2) => U): U = {
    try {
      body(resource1, resource2)
    } finally {
      closeQuietly(resource1)
      closeQuietly(resource2)
    }
  }

  private def closeQuietly(resource: AutoCloseable): Unit = {
    if (resource != null) {
      try {
        resource.close
      } catch {
        case NonFatal(_) => () // ignore closing exception
      }
    }
  }

}
