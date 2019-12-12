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
package wvlet.airframe.http.finagle.filter
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.filter.Cors.Policy
import wvlet.airframe.http.finagle.FinagleBackend

object CorsFilter {
  def apply(policy: Policy) = FinagleBackend.wrapFilter(new Cors.HttpFilter(policy))

  // CORS filter that allows all cross-origin requests. Do not use this in production.
  def unsafePermissiveFilter = apply(Cors.UnsafePermissivePolicy)
}
