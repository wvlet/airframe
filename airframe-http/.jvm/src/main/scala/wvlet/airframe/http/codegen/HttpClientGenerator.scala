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
import java.io.{File, FileWriter}
import java.net.URLClassLoader

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.control.Control
import wvlet.airframe.http.Router
import wvlet.airframe.http.codegen.HttpClientGenerator.{Artifacts, generate}
import wvlet.airframe.http.codegen.client.{AsyncClient, HttpClientType}
import wvlet.airframe.launcher.Launcher
import wvlet.log.io.IOUtil
import wvlet.log.{LogSupport, Logger}

case class HttpClientGeneratorConfig(
    // A package name to search for airframe-http interfaces
    apiPackageName: String,
    // scala-async, scala-sync, scala-js, etc.
    clientType: HttpClientType = AsyncClient,
    // [optional] Which package to use for the generating client code?
    targetPackageName: String
) {
  def fileName = clientType.defaultFileName
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

  def generate(config: HttpClientGeneratorConfig, cl: ClassLoader): String = {
    val router = RouteScanner.buildRouter(Seq(config.apiPackageName), cl)
    val code   = generate(router, config)
    code
  }

  def main(args: Array[String]): Unit = {
    Launcher.of[HttpClientGenerator].execute(args)
  }

  case class Artifacts(file: Seq[File])
}

import wvlet.airframe.launcher._

class HttpClientGenerator(
    @option(prefix = "-h,--help", description = "show help message", isHelp = true)
    isHelp: Boolean = false
) extends LogSupport {
  Logger.init

  @command(isDefault = true)
  def default = {
    info(s"Type --help for the available options")
  }

  @command(description = "Generate HTTP client codes")
  def generate(
      @option(prefix = "-cp", description = "semi-colon separated application classpaths")
      classpath: String = "",
      @option(prefix = "-o", description = "output base directory")
      outDir: File,
      @option(prefix = "-t", description = "working directory")
      targetDir: File,
      @argument(description = "client code generation targets: (package):(type)(:(targetPackage))?")
      targets: Seq[String] = Seq.empty
  ): Unit = {
    val cp = classpath.split(":").map(x => new File(x).toURI.toURL).toArray
    val cl = new URLClassLoader(cp, Thread.currentThread().getContextClassLoader)
    val artifacts = for (x <- targets) yield {
      val config = HttpClientGeneratorConfig(x)
      info(config)
      val router    = RouteScanner.buildRouter(Seq(config.apiPackageName), cl)
      val routerStr = router.toString
      info(s"Found a router for package ${config.apiPackageName}:\n${routerStr}")
      val routerHash = routerStr.hashCode

      if (!targetDir.exists()) {
        targetDir.mkdirs()
      }
      val routerHashFile = new File(targetDir, f"router-${routerHash}%07x.update")

      val path       = s"${config.targetPackageName.replaceAll("\\.", "/")}/${config.fileName}"
      val outputFile = new File(outDir, path)
      outputFile.getParentFile.mkdirs()

      if (!(outputFile.exists() && routerHashFile.exists())) {
        val code = HttpClientGenerator.generate(router, config)

        info(s"Generating a ${config.clientType.name} client code: ${path}\n${code}")
        writeFile(outputFile, code)
        touch(routerHashFile)
      } else {
        info(s"${outputFile} is up-to-date")
      }
      outputFile
    }
    println(MessageCodec.of[Seq[File]].toJson(artifacts))
  }

  private def touch(f: File): Unit = {
    f.setLastModified(System.currentTimeMillis())
  }

  private def writeFile(outputFile: File, data: String): Unit = {
    Control.withResource(new FileWriter(outputFile)) { out => out.write(data); out.flush() }
  }

}
