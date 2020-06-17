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
package wvlet.airframe.http.rx.widget.ui

import wvlet.airframe.http.rx.html.all._
import wvlet.airframe.http.rx.html.{RxComponent, RxElement}

/**
  */
object Layout {
  def of                                     = div()
  def divOf(primaryClass: String): RxElement = div(_class -> primaryClass)

  def codeBlock: RxComponent = RxComponent { content => pre(code(content)) }

  def scalaCode: RxComponent = RxComponent { content => pre(code(_class -> "language-scala rounded", content)) }
}
