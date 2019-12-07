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
package wvlet.airframe.http.finagle
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.Design
import wvlet.airframe.http.Router
import wvlet.airframe.http.finagle.CorsFilterTest.{LogFilter, SimpleFilter}
import wvlet.airframe.http.finagle.filter.CorsFilter
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

object CorsFilterTest {

  object LogFilter extends FinagleFilter with LogSupport {
    override def apply(
        request: Request,
        context: LogFilter.Context
    ): Future[Response] = {
      debug(s"request: ${request}")
      context(request)
    }
  }

  object SimpleFilter extends FinagleFilter {
    override def apply(
        request: Request,
        context: SimpleFilter.Context
    ): Future[Response] = {
      toFuture {
        val r = Response()
        r.contentString = request.headerMap.mkString(", ")
        r
      }
    }
  }
}

/**
  *
  */
class CorsFilterTest extends AirSpec {

  override protected def design: Design = {
    val r = Router
      .add(LogFilter)
      .andThen(CorsFilter.unsafePermissibleFilter)
      .andThen(SimpleFilter)

    debug(r)

    newFinagleServerDesign(router = r)
      .add(finagleSyncClientDesign)
  }

  def `support CORS filter`(client: FinagleSyncClient): Unit = {
    val resp = client.get[String]("/")
    debug(resp)
  }

}
