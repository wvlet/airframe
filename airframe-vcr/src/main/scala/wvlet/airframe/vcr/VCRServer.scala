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
package wvlet.airframe.vcr
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.http.finagle.FinagleServer.FinagleService

/**
  * VCR server
  */
class VCRServer(vcrRecorder: VCRRecorder, fallback: Service[Request, Response]) extends FinagleService {

  override def apply(request: Request): Future[Response] = {
    vcrRecorder.find(request) match {
      case None =>
        fallback(request).map { response =>
          // Record the result
          vcrRecorder.record(request, response)
          response
        }
      case Some(vcr) =>
        Future.value(vcr.toResponse)
    }
  }
}
