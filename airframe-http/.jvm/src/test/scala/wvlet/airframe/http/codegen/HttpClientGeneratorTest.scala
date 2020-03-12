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
package wvlet.airframe.http.codegen
import wvlet.airspec.AirSpec
import example._
import wvlet.airframe.http._
import java.net.URLClassLoader

import wvlet.airframe.http.codegen.client._

/**
  *
  */
class HttpClientGeneratorTest extends AirSpec {
  val router =
    RouteScanner.buildRouter(Seq(classOf[ResourceApi], classOf[QueryApi]))

  test("build router") {
    debug(router)

    val r = router.routes.find(x => x.method == HttpMethod.GET && x.path == "/v1/resources/:id")
    r shouldBe defined

    val q = router.routes.find(x => x.method == HttpMethod.GET && x.path == "/v1/query")
    q shouldBe defined
  }

  test("generate async client") {
    val code = HttpClientGenerator.generate(
      router,
      HttpClientGeneratorConfig("example.api:async:example.api.client")
    )
    code.contains("package example.api.client") shouldBe true
    code.contains("class ServiceClient[F[_], Req, Resp]")
  }

  test("generate sync client") {
    val code = HttpClientGenerator.generate(
      router,
      HttpClientGeneratorConfig("example.api:sync")
    )
    code.contains("package example.api") shouldBe true
    code.contains("class ServiceSyncClient[Req, Resp]")
  }

  test("generate Scala.js client") {
    val code = HttpClientGenerator.generate(
      router,
      HttpClientGeneratorConfig("example.api:scalajs:example.api.client.js")
    )
    code.contains("package example.api.client.js") shouldBe true
    code.contains("object ServiceJSClient")
  }

  test("scan classes") {
    skip("This is a test used only for debugging")
    val urls = Array[java.net.URL](
      new java.net.URL("file:./sbt-airframe/target/scala-2.12/sbt-1.0/test-classes/"),
      new java.net.URL(
        "file:/Users/leo/.coursier/cache/v1/https/repo1.maven.org/maven2/org/wvlet/airframe/airframe-json_2.12/20.2.1/airframe-json_2.12-20.2.1.jar"
      )
    )
    val cl      = new URLClassLoader(urls)
    val classes = ClassScanner.scanClasses(cl, Seq("example", "wvlet.airframe.json"))
    debug(classes)
  }
}
