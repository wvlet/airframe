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
package wvlet.airframe.http.client

import wvlet.airframe.http.RxHttpFilter

/**
  * A filter for intercepting HTTP requests by using
  */
trait HttpClientFilter { self =>
  def apply(context: HttpClientContext): RxHttpFilter

  def andThen(next: HttpClientFilter): HttpClientFilter = new HttpClientFilter {
    override def apply(context: HttpClientContext): RxHttpFilter = {
      self.apply(context).andThen(next(context))
    }
  }
}

object HttpClientFilter {
  def identity: HttpClientFilter = new HttpClientFilter {
    override def apply(context: HttpClientContext): RxHttpFilter = RxHttpFilter.identity
  }
}
