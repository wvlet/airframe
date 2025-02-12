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
package wvlet.airframe.test.api

import wvlet.airframe.http.*
import wvlet.airframe.rx.Rx

@RPC
trait HelloRPC:
  import HelloRPC.*

  def hello(name: String): String
  def serverStatus: Status
  def ackStatus(status: Status): Status
  def variousParams(params: VariousParams): VariousParams
  def ackStatusAsync(name: String): Rx[Status]

object HelloRPC extends RxRouterProvider:
  override def router: RxRouter = RxRouter.of[HelloRPC]

  case class VariousParams(
      p1: Long,
      p2: Boolean,
      p3: Double
  )

enum Status(isDone: Boolean):
  def name: String = this.toString
  case OK extends Status(isDone = true)
  case NG extends Status(isDone = true)
