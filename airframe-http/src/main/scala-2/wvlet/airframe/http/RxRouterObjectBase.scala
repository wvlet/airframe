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
package wvlet.airframe.http
import scala.language.experimental.macros

trait RxRouterObjectBase {
  def of[Controller]: RxRouter = macro RxRouterMacros.of[Controller]
  def filter[Filter <: RxHttpFilter]: RxRouter.FilterNode = macro RxRouterMacros.filter[Filter]
  def filter[Filter <: RxHttpFilter](filterInstance: Filter): RxRouter.FilterNode =
    macro RxRouterMacros.filterInstance[Filter]
}

trait RxRouteFilterBase {
  def andThen[Filter <: RxHttpFilter]: RxRouter.FilterNode = macro RxRouterMacros.andThenFilter[Filter]
  def andThen[Filter <: RxHttpFilter](filterInstance: Filter): RxRouter.FilterNode =
    macro RxRouterMacros.andThenFilterInstance[Filter]
}
