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
import wvlet.airframe.http.{HttpFilter, Router}
import wvlet.airspec.AirSpec

object LeafFilterTest {
  class MyFilter extends FinagleFilter {
    override def apply(request: Request, context: Context): Future[Response] = {
      val r = Response()
      r.contentString = "leaf filter"
      toFuture(r)
    }
  }
}

/**
  */
class LeafFilterTest extends AirSpec {
  import LeafFilterTest._

  override protected def design: Design = {
    newFinagleServerDesign(name = "leaf-filter-test", router = Router.add[MyFilter])
      .add(finagleSyncClientDesign)
  }

  def `support leaf filters`(client: FinagleSyncClient): Unit = {
    client.get[String]("/") shouldBe "leaf filter"
  }

}
