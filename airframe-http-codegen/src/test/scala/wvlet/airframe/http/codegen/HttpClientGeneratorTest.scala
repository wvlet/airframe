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
import java.net.URLClassLoader
import example.api._
import wvlet.airframe.http._
import wvlet.airframe.http.codegen.client.{AsyncClientGenerator, GrpcClientGenerator}
import wvlet.airspec.AirSpec

/**
  */
class HttpClientGeneratorTest extends AirSpec {
  val router =
    RouteScanner.buildRouter(Seq(classOf[ResourceApi], classOf[QueryApi], classOf[BookApi]))

  test("build router") {
    debug(router)

    val r = router.routes.find(x => x.method == HttpMethod.GET && x.path == "/v1/resources/:id")
    r shouldBe defined

    val q = router.routes.find(x => x.method == HttpMethod.GET && x.path == "/v1/query")
    q shouldBe defined
  }

  test("customize RPC client") {
    test("custom class name") {
      val config = HttpClientGeneratorConfig("example.api:async:MyApiClient")
      config.apiPackageName shouldBe "example.api"
      config.clientType shouldBe AsyncClientGenerator
      config.targetClassName shouldBe Some("MyApiClient")
      config.clientFileName shouldBe "MyApiClient.scala"
      config.targetPackageName shouldBe "example.api"

      val code = HttpCodeGenerator.generate(router, config)
      code.contains("package example.api") shouldBe true
      code.contains("class MyApiClient") shouldBe true
    }

    test("custom package and class name") {
      val config = HttpClientGeneratorConfig("example.api:grpc:example.api.client.MyApiClient")
      config.apiPackageName shouldBe "example.api"
      config.clientType shouldBe GrpcClientGenerator
      config.targetClassName shouldBe Some("MyApiClient")
      config.clientFileName shouldBe "MyApiClient.scala"
      config.targetPackageName shouldBe "example.api.client"

      val code = HttpCodeGenerator.generate(router, config)
      code.contains("package example.api.client") shouldBe true
      // grpc target generates client generator in a Scala object
      code.contains("object MyApiClient") shouldBe true
    }

    test("customize only package") {
      val config = HttpClientGeneratorConfig("example.api:grpc:example.api.client")
      config.apiPackageName shouldBe "example.api"
      config.clientType shouldBe GrpcClientGenerator
      config.targetClassName shouldBe None
      config.clientFileName shouldBe s"${GrpcClientGenerator.defaultClassName}.scala"
      config.targetPackageName shouldBe "example.api.client"

      val code = HttpCodeGenerator.generate(router, config)
      code.contains("package example.api.client") shouldBe true
      code.contains(s"object ${GrpcClientGenerator.defaultClassName}") shouldBe true
    }

  }

  test("generate async client") {
    val code = HttpCodeGenerator.generate(
      router,
      HttpClientGeneratorConfig("example.api:async:example.api.client")
    )
    code.contains("package example.api.client") shouldBe true
    code.contains("import example.api.Query") shouldBe false
    code.contains("class ServiceClient[F[_], Req, Resp]") shouldBe true
    code.contains("import scala.collection.Seq") shouldBe false
    code.contains("import scala.language.higherKinds") shouldBe true

    code.contains("HttpRequest[Request]") shouldBe false

    test("Map client parameters to GET query strings") {
      code.contains("def getBooks(limit: Int") shouldBe true
    }
  }

  test("generate sync client") {
    val code = HttpCodeGenerator.generate(
      router,
      HttpClientGeneratorConfig("example.api:sync")
    )
    code.contains("package example.api") shouldBe true
    code.contains("class ServiceSyncClient[Req, Resp]")
  }

  test("generate Scala.js client") {
    val code = HttpCodeGenerator.generate(
      router,
      HttpClientGeneratorConfig("example.api:scalajs:example.api.client.js")
    )
    code.contains("package example.api.client.js") shouldBe true
    code.contains("class ServiceJSClient") shouldBe true

    test("Map client parameters to GET query strings") {
      code.contains("def getBooks(limit: Int") shouldBe true
      code.contains("""Map("limit" -> limit, "sort" -> sort)""") shouldBe true
    }
    code.contains("import java.lang.Object") shouldBe false

    test("generate ServiceJSClientRx") {
      code.contains("class ServiceJSClientRx") shouldBe true
      code.contains(": RxStream[scala.collection.Seq[example.Book]] = ")
    }
  }

  test("scan classes") {
    skip("This is a test used only for debugging")
    val urls = Array[java.net.URL](
      new java.net.URL("file:./sbt-airframe/target/scala-2.12/sbt-1.0/test-classes/"),
      new java.net.URL(
        "file:/Users/leo/.coursier/cache/v1/https/repo1.maven.org/maven2/org/wvlet/airframe/airframe-json_2.12/20.2.1/airframe-json_2.12-20.2.1.jar"
      )
    )
    val cl = new URLClassLoader(urls)
    val classes =
      ClassScanner.scanClasses(cl, Seq("example", "wvlet.airframe.json"))
    debug(classes)
  }

  test("generate clients inside a package object") {
    val code = HttpCodeGenerator.generate(
      Router.of[example.service.PkgService],
      HttpClientGeneratorConfig("example.service:sync")
    )
    debug(code)

    code.contains(": example.service.Node") shouldBe true
    code.contains("example.service.package.Node") shouldBe false
  }

}
