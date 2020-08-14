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
package wvlet.airframe.http.rx

/**
  */
sealed trait RxEvent {
  def isLastEvent: Boolean
  def isError: Boolean
}
case class OnNext(v: Any) extends RxEvent {
  override def isLastEvent: Boolean = false
  override def isError: Boolean     = false
}
case class OnError(e: Throwable) extends RxEvent {
  override def isLastEvent: Boolean = true
  override def isError: Boolean     = true
}
case object OnCompletion extends RxEvent {
  override def isLastEvent: Boolean = true
  override def isError: Boolean     = false
}
