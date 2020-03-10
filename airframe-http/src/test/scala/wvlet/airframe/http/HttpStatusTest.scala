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

import wvlet.airspec.AirSpec

/**
  *
  */
class HttpStatusTest extends AirSpec {
  test("have reasons") {
    HttpStatus.knownStatuses.foreach { x =>
      x.toString
      HttpStatus.ofCode(x.code) == x
    }

    (100 to 600).foreach { code =>
      val s = HttpStatus.ofCode(code)
      s.reason // sanity test

      // Status code check
      s.isUnknownState shouldBe (code < 100 || code >= 600)
      s.isInformational shouldBe (100 <= code && code < 200)
      s.isSuccessful shouldBe (200 <= code && code < 300)
      s.isRedirection shouldBe (300 <= code && code < 400)
      s.isClientError shouldBe (400 <= code && code < 500)
      s.isServerError shouldBe (500 <= code && code < 600)
    }
  }
}
