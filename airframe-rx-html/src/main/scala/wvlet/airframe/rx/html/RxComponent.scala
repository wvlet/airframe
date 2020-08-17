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

/**
  */
trait RxComponent {
  def render(content: RxElement): RxElement

  def apply(elems: RxElement*): RxElement = {
    elems.size match {
      case 1     => LazyRxElement(() => render(elems.head))
      case other => LazyRxElement(() => render(Embedded(elems.toSeq)))
    }
  }
}

object RxComponent {

  def ofTag(name: String): RxComponent =
    new RxComponent { content =>
      override def render(content: RxElement): RxElement = {
        tag(name)(content)
      }
    }

  def apply(f: RxElement => RxElement): RxComponent =
    new RxComponent {
      override def render(content: RxElement): RxElement = {
        f(content)
      }
    }
}
