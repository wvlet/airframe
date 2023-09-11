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
package wvlet.airframe.rx.html

import wvlet.log.LogSupport

/**
  * Holder for embedding various types as tag contents
  *
  * @param v
  */
private[html] case class Embedded(v: Any) extends RxElement with LogSupport {
  override def render: RxElement = {
    warn(s"render is called for a placeholder ${v}")
    ???
  }
}

private[rx] case class RxCode(rxElements: Seq[RxElement], sourceCode: String)
