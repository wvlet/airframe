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
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.Design
import wvlet.airframe.http.Router
import wvlet.airframe.http.finagle.CorsFilterTest.{CheckFilter, LogFilter, SimpleFilter}
import wvlet.airframe.http.finagle.filter.CorsFilter
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

object CorsFilterTest {

  // Test SimpleFilter of Finagle
  object CheckFilter extends SimpleFilter[Request, Response] with LogSupport {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      info(s"checking request: ${request}")
      service(request)
    }
  }

  object LogFilter extends FinagleFilter with LogSupport {
    override def apply(
        request: Request,
        context: LogFilter.Context
    ): Future[Response] = {
      info(s"logging request: ${request}")
      context(request)
    }
  }

  object SimpleFilter extends FinagleFilter with LogSupport {
    override def apply(
        request: Request,
        context: SimpleFilter.Context
    ): Future[Response] = {
      toFuture {
        info(s"handling request: ${request}")
        val r = Response()
        r.contentString = request.headerMap.mkString(", ")
        r
      }
    }
  }
}

/**
  */
class CorsFilterTest extends AirSpec {

  override protected def design: Design = {
    val r = Router
      .add(LogFilter)
      .andThen(FinagleBackend.wrapFilter(CheckFilter))
      .andThen(CorsFilter.unsafePermissiveFilter)
      .andThen(SimpleFilter)

    debug(r)

    newFinagleServerDesign(router = r)
      .add(finagleSyncClientDesign)
  }

  test("support CORS filter") { (client: FinagleSyncClient) =>
    val resp = client.get[String]("/")
    debug(resp)
  }

}
