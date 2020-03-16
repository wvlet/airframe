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

import wvlet.airframe.http.Router
import wvlet.airframe.http.codegen.client.{AsyncClient, HttpClientType}
import wvlet.log.LogSupport

case class HttpClientGeneratorConfig(
    // A package name to search for airframe-http interfaces
    apiPackageName: String,
    // scala-async, scala-sync, scala-js, etc.
    clientType: HttpClientType = AsyncClient,
    // [optional] Which package to use for the generating client code?
    targetPackageName: String
) {
  def className = clientType.defaultClassName
}

object HttpClientGeneratorConfig {

  def apply(s: String): HttpClientGeneratorConfig = {
    // Parse strings of (package):(type)(:(targetPackage))? format. For example:
    //    "example.api:async:example.api.client"
    //    "example.api:sync"
    val (packageName, tpe, targetPackage) = s.split(":") match {
      case Array(p, tpe, clsName) =>
        (p, tpe, clsName)
      case Array(p, tpe) =>
        (p, tpe, p)
      case Array(p) =>
        (p, "async", p)
      case _ =>
        throw new IllegalArgumentException(s"Invalid argument: ${s}")
    }

    HttpClientGeneratorConfig(
      apiPackageName = packageName,
      clientType = HttpClientType.findClient(tpe).getOrElse {
        throw new IllegalArgumentException(s"Unknown client type: ${tpe}")
      },
      targetPackageName = targetPackage
    )
  }
}

/**
  * Generate HTTP client code for Scala, Scala.js targets using a given IR
  */
object HttpClientGenerator extends LogSupport {
  def generate(
      router: Router,
      config: HttpClientGeneratorConfig
  ): String = {

    val ir   = HttpClientIR.buildIR(router, config)
    val code = config.clientType.generate(ir)
    debug(code)
    code
  }

  def generate(config: HttpClientGeneratorConfig, cl: URLClassLoader): String = {
    val router = RouteScanner.buildRouter(Seq(config.apiPackageName), cl)
    val code   = generate(router, config)
    code
  }
}
