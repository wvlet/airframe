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

import wvlet.airframe.test.api.HelloRPC.VariousParams
import wvlet.airframe.test.api.Status
import wvlet.log.LogSupport

class HelloRPCImpl extends HelloRPC with LogSupport {
  override def hello(name: String): String = s"Hello ${name}!"

  override def serverStatus: Status = Status.OK
  override def ackStatus(status: Status): Status = {
    info(s"acked: ${status}")
    status
  }

  override def variousParams(params: VariousParams): VariousParams = {
    info(s"received: ${params}")
    params
  }
}
