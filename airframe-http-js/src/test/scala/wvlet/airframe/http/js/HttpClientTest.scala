package wvlet.airframe.http.js

import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

object HttpClientTest extends AirSpec {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  case class Person(id: Int, name: String)

  test("create http client") {
    ignore("ignore server interaction tests")
    val s = Surface.of[Person]
    HttpClient.getOps[Person, Person]("/v1/info", Person(1, "leo"), s, s).recover {
      case e: Throwable =>
        logger.warn(e)
        1
    }
  }
}
