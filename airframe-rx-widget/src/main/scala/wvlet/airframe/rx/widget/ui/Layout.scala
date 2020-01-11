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
package wvlet.airframe.rx.widget.ui
import wvlet.airframe.rx.widget.{RxComponent, RxElement}

/**
  *
  */
object Layout {
  def of: RxComponent  = div
  def div: RxComponent = RxComponent.ofTag("div")
  def divOf(primaryClass: String): RxComponent = RxComponent { content =>
    <div class={primaryClass}>{content}</div>
  }

  def h1: RxComponent = RxComponent.ofTag(tag = "h1")
  def h2: RxComponent = RxComponent.ofTag(tag = "h2")
  def h3: RxComponent = RxComponent.ofTag(tag = "h3")
  def h4: RxComponent = RxComponent.ofTag(tag = "h4")
  def h5: RxComponent = RxComponent.ofTag(tag = "h5")

  def p: RxComponent = RxComponent.ofTag(tag = "p")

  def code(codeStr: String): RxElement = RxElement {
    <code>{codeStr}</code>
  }

  def codeBlock: RxComponent = RxComponent { content =>
    <pre><code>{content}</code></pre>
  }

  def scalaCode: RxComponent = RxComponent { content =>
    <pre><code class="language-scala rounded">{content}</code></pre>
  }
}
