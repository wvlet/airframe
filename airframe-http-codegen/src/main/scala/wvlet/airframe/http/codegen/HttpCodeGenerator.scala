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
import wvlet.airframe.http.codegen.client.{AsyncClientGenerator, HttpClientGenerator}
import wvlet.airframe.http.openapi.{OpenAPI, OpenAPIGeneratorConfig}
import wvlet.airframe.launcher.Launcher
import wvlet.log.io.IOUtil
import wvlet.log.{LogLevel, LogSupport, Logger}

case class HttpClientGeneratorConfig(
    // A package name to search for airframe-http interfaces
    apiPackageName: String,
    // scala-async, scala-sync, scala-js, etc.
    clientType: HttpClientGenerator = AsyncClientGenerator,
    // [optional] Which package to use for the generating client code?
    targetPackageName: String,
    private[codegen] val targetClassName: Option[String]
) {
  def clientClassName: String = targetClassName.getOrElse(clientType.defaultClassName)
  def clientFileName: String  = s"${clientClassName}.scala"
}

object HttpClientGeneratorConfig {

  def apply(s: String): HttpClientGeneratorConfig = {
    // Parse strings of (package):(type)(:(targetPackageName(.targetClassName)?)? format. For example:
    //    "example.api:async:example.api.client"
    //    "example.api:sync"
    //    "example.api:grpc:MyGrpcClient"
    //    "example.api:scalajs:example.api.client.MyJSClient"
    val (packageName, tpe, targetPackage, targetCls) = s.split(":") match {
      case Array(p, tpe, pkgName) =>
        val lst = pkgName.split("\\.")
        // Assume that it's a client class name if the first letter is capital
        if (lst.last.matches("^[A-Z].*")) {
          val clsName = lst.lastOption
          if (lst.length == 1) {
            // If only a class name is given, use the same package with API
            (p, tpe, p, clsName)
          } else {
            (p, tpe, lst.dropRight(1).mkString("."), clsName)
          }
        } else {
          (p, tpe, pkgName, None)
        }
      case Array(p, tpe) =>
        (p, tpe, p, None)
      case Array(p) =>
        // Generate async client by default
        (p, "async", p, None)
      case _ =>
        throw new IllegalArgumentException(s"Invalid argument: ${s}")
    }

    HttpClientGeneratorConfig(
      apiPackageName = packageName,
      clientType = HttpClientGenerator.findClient(tpe).getOrElse {
        throw new IllegalArgumentException(s"Unknown client type: ${tpe}")
      },
      targetPackageName = targetPackage,
      targetClassName = targetCls
    )
  }
}

/**
  * Generate HTTP client code for Scala, Scala.js targets using a given IR
  */
object HttpCodeGenerator extends LogSupport {
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

  def generateOpenAPI(
      router: Router,
      formatType: String,
      title: String,
      version: String,
      packageNames: Seq[String]
  ): String = {
    val openapi = OpenAPI
      .ofRouter(router, OpenAPIGeneratorConfig(basePackages = packageNames)).withInfo(
        OpenAPI.Info(title = title, version = version)
      )
    val schema = formatType match {
      case "yaml" =>
        openapi.toYAML
      case "json" =>
        openapi.toJSON
      case other =>
        throw new IllegalArgumentException(s"Unknown file format type: ${other}. Required yaml or json")
    }
    schema
  }

  def main(args: Array[String]): Unit = {
    Launcher.of[HttpCodeGenerator].execute(args)
  }

  case class Artifacts(file: Seq[File])

}

import wvlet.airframe.launcher._

case class HttpCodeGeneratorOption(
    @option(prefix = "-cp", description = "application classpaths")
    classpath: Seq[String] = Seq.empty,
    @option(prefix = "-o", description = "output base directory")
    outDir: File,
    @option(prefix = "-t", description = "target directory")
    targetDir: File,
    @argument(description = "client code generation targets: (package):(type)(:(targetPackageName)(.tagetClassName)?)?")
    targets: Seq[String] = Seq.empty
)

case class OpenAPIGeneratorOption(
    @option(prefix = "-cp", description = "application classpaths")
    classpath: Seq[String] = Seq.empty,
    @option(prefix = "-o", description = "output file")
    outFile: File,
    @option(prefix = "-f", description = "format type: yaml (default) or json")
    formatType: String = "yaml",
    @option(prefix = "--title", description = "openapi.title")
    title: String,
    @option(prefix = "--version", description = "openapi.version")
    version: String,
    @argument(description = "Target Airframe HTTP/RPC package name")
    packageNames: Seq[String]
)

class HttpCodeGenerator(
    @option(prefix = "-h,--help", description = "show help message", isHelp = true)
    isHelp: Boolean = false,
    @option(prefix = "-l,--loglevel", description = "log level")
    logLevel: Option[LogLevel] = None
) extends LogSupport {
  Logger.init

  logLevel.foreach { x =>
    Logger("wvlet.airframe.http").setLogLevel(x)
  }

  @command(isDefault = true)
  def default = {
    info(s"Type --help for the available options")
  }

  private def newClassLoader(classpath: String): URLClassLoader = {
    val cp = classpath.split(":").map(x => new File(x).toURI.toURL).toArray
    new URLClassLoader(cp)
  }

  private def buildRouter(apiPackageNames: Seq[String], classLoader: URLClassLoader): Router = {
    info(s"Target API packages: ${apiPackageNames.mkString(", ")}")
    val router = RouteScanner.buildRouter(apiPackageNames, classLoader)
    router
  }

  @command(description = "Generate HTTP client code using a JSON configuration file")
  def generateFromJson(
      @argument(description = "HttpCodeGeneratorOption in JSON file")
      jsonFilePath: String
  ): Unit = {
    info(s"Reading JSON option file: ${jsonFilePath}")
    val option = MessageCodec.of[HttpCodeGeneratorOption].fromJson(IOUtil.readAsString(jsonFilePath))
    generate(option)
  }

  @command(description = "Generate HTTP client codes")
  def generate(option: HttpCodeGeneratorOption): Unit = {
    try {
      val cl = newClassLoader(option.classpath.mkString(":"))
      val artifacts = for (x <- option.targets) yield {
        val config = HttpClientGeneratorConfig(x)
        debug(config)
        if (!option.targetDir.exists()) {
          option.targetDir.mkdirs()
        }
        val path       = s"${config.targetPackageName.replaceAll("\\.", "/")}/${config.clientFileName}"
        val outputFile = new File(option.outDir, path)

        val router         = buildRouter(Seq(config.apiPackageName), cl)
        val routerStr      = router.toString
        val routerHash     = routerStr.hashCode
        val routerHashFile = new File(option.targetDir, f"router-${config.clientType.name}-${routerHash}%07x.update")
        if (!outputFile.exists() || !routerHashFile.exists()) {
          info(f"Router for package ${config.apiPackageName}:\n${routerStr}")
          info(s"Generating a ${config.clientType.name} client code: ${path}")
          val code = HttpCodeGenerator.generate(router, config)
          touch(routerHashFile)
          writeFile(outputFile, code)
        } else {
          info(s"${outputFile} is up-to-date")
        }
        outputFile
      }
      println(MessageCodec.of[Seq[File]].toJson(artifacts))
    } catch {
      case e: Throwable =>
        warn(e)
        println("[]") // empty result
    }
  }

  @command(description = "Generate OpenAPI spec using a JSON configuration file")
  def openapiFromJson(
      @argument(description = "HttpCodeGeneratorOpenAPIOption in JSON file")
      jsonFilePath: String
  ): Unit = {
    val option = MessageCodec.of[OpenAPIGeneratorOption].fromJson(IOUtil.readAsString(jsonFilePath))
    openapi(option)
  }

  @command(description = "Generate OpenAPI spec")
  def openapi(
      option: OpenAPIGeneratorOption
  ): Unit = {
    trace(s"classpath: ${option.classpath.mkString(":")}")
    val router = buildRouter(option.packageNames, newClassLoader(option.classpath.mkString(":")))
    debug(router)
    val schema =
      HttpCodeGenerator.generateOpenAPI(router, option.formatType, option.title, option.version, option.packageNames)
    debug(schema)
    info(s"Writing OpenAPI spec ${option.formatType} to ${option.outFile.getPath}")
    writeFile(option.outFile, schema)
  }

  private def touch(f: File): Unit = {
    if (!f.createNewFile()) {
      f.setLastModified(System.currentTimeMillis())
    }
  }

  private def writeFile(outputFile: File, data: String): Unit = {
    outputFile.getParentFile.mkdirs()
    Control.withResource(new FileWriter(outputFile)) { out =>
      out.write(data); out.flush()
    }
  }

}
