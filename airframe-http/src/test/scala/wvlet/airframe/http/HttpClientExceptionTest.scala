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

import wvlet.airframe.control.ResultClass
import wvlet.airspec.AirSpec

class HttpClientExceptionTest extends AirSpec {

  private def assertIsRetryable(response: HttpMessage.Response, expected: Boolean): Unit = {
    val result = HttpClientException.classifyHttpResponse(response)
    result match {
      case ResultClass.Failed(isRetryable, _, _) =>
        withClue(s"For status ${response.status}") {
          isRetryable shouldBe expected
        }
      case _ =>
        fail(s"Expected Failed result for ${response.status}")
    }
  }

  test("classify 304 Not Modified as success") {
    val response = HttpMessage.Response(HttpStatus.NotModified_304)
    val result   = HttpClientException.classifyHttpResponse(response)
    result shouldBe ResultClass.Succeeded
  }

  test("classify 2xx responses as success") {
    val responses = Seq(
      HttpMessage.Response(HttpStatus.Ok_200),
      HttpMessage.Response(HttpStatus.Created_201),
      HttpMessage.Response(HttpStatus.Accepted_202),
      HttpMessage.Response(HttpStatus.NoContent_204)
    )

    responses.foreach { response =>
      val result = HttpClientException.classifyHttpResponse(response)
      result shouldBe ResultClass.Succeeded
    }
  }

  test("classify server errors as retryable") {
    val responses = Seq(
      HttpMessage.Response(HttpStatus.InternalServerError_500),
      HttpMessage.Response(HttpStatus.BadGateway_502),
      HttpMessage.Response(HttpStatus.ServiceUnavailable_503)
    )

    responses.foreach(assertIsRetryable(_, expected = true))
  }

  test("classify most client errors as non-retryable") {
    val responses = Seq(
      HttpMessage.Response(HttpStatus.BadRequest_400),
      HttpMessage.Response(HttpStatus.Unauthorized_401),
      HttpMessage.Response(HttpStatus.Forbidden_403),
      HttpMessage.Response(HttpStatus.NotFound_404)
    )

    responses.foreach(assertIsRetryable(_, expected = false))
  }

  test("classify specific client errors as retryable") {
    val retryableClientErrors = Seq(
      HttpMessage.Response(HttpStatus.RequestTimeout_408),
      HttpMessage.Response(HttpStatus.Gone_410),
      HttpMessage.Response(HttpStatus.TooManyRequests_429),
      HttpMessage.Response(HttpStatus.ClientClosedRequest_499)
    )

    retryableClientErrors.foreach(assertIsRetryable(_, expected = true))
  }
}
