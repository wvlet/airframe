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

import wvlet.airframe.http._

@RPC
trait HelloRPC {
  import HelloRPC._

  def hello(name: String): String
  def serverStatus: Status
  def ackStatus(status: Status): Status
  def variousParams(params: VariousParams): VariousParams
}

object HelloRPC extends RxRouterProvider {
  override def router: RxRouter = RxRouter.of[HelloRPC]

  case class VariousParams(
      p1: Long,
      p2: Boolean,
      p3: Double
  )
}

sealed trait Status {
  def isDone: Boolean

  def name: String = toString
}

object Status {
  case object OK extends Status {
    override def isDone: Boolean = true
  }

  case object NG extends Status {
    override def isDone: Boolean = true
  }

  def all: Seq[Status] = Seq(OK, NG)

  def unapply(s: String): Option[Status] = {
    all.find(_.toString == s)
  }
}
