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
package wvlet.airframe.http.codegen.client

import wvlet.airframe.http.codegen.HttpClientIR.{ClientMethodDef, ClientServiceDef, ClientSourceDef}
import wvlet.log.LogSupport

/**
  */
object ScalaHttpClientGenerator {
  def header(packageName: String): String = {
    s"""/**
       | * === DO NOT EDIT THIS FILE ===
       | * This code is generated by sbt-airframe plugin.
       | */
       |package ${packageName}""".stripMargin
  }

  def indent(body: String, level: Int = 1): String = {
    body
      .split("\n")
      .map { x =>
        if (x.trim.isEmpty) {
          ""
        } else {
          s"${"  " * level}${x}"
        }
      }
      .mkString("\n")
  }
}

import ScalaHttpClientGenerator._

object AsyncClientGenerator extends HttpClientGenerator with LogSupport {

  import HttpClientGenerator._

  override def name: String             = "async"
  override def defaultClassName: String = "ServiceClient"
  override def generate(src: ClientSourceDef): String = {
    def code =
      s"""${header(src.destPackageName)}
         |
         |import wvlet.airframe.http._
         |import scala.language.higherKinds
         |
         |${cls}""".stripMargin.stripMargin

    def cls: String =
      s"""class ${src.classDef.clsName}[F[_], Req, Resp](private val client: HttpClient[F, Req, Resp]) extends AutoCloseable {
         |  override def close(): Unit = { client.close() }
         |  def getClient: HttpClient[F, Req, Resp] = client
         |${indent(clsBody)}
         |}
         |""".stripMargin

    def clsBody: String = {
      generateNestedStub(src) { svc =>
        s"""object ${svc.serviceName} {
           |${indent(serviceBody(svc))}
           |}""".stripMargin
      }
    }

    def serviceBody(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters
              .map { x =>
                s"${x.name}: ${x.surface.fullTypeName}" ++ Seq("requestFilter: Req => Req = identity")
              }

          val sendRequestArgs = Seq.newBuilder[String]
          sendRequestArgs += s"""resourcePath = s"${m.path}""""
          sendRequestArgs ++= m.clientCallParameters
          sendRequestArgs += "requestFilter = requestFilter"

          val lines = Seq.newBuilder[String]
          m.requestModelClassDef.foreach { x =>
            lines += x.code()
          }
          lines += s"def ${m.name}(${inputArgs.mkString(", ")}): F[${m.returnType.fullTypeName}] = {"
          lines += s"  client.${m.clientMethodName}[${m.typeArgString}](${sendRequestArgs.result().mkString(", ")})"
          lines += s"}"
          lines.result().mkString("\n")
        }
        .mkString("\n")
    }

    code
  }
}

object SyncClientGenerator extends HttpClientGenerator {

  import HttpClientGenerator._

  override def name: String             = "sync"
  override def defaultClassName: String = "ServiceSyncClient"
  override def generate(src: ClientSourceDef): String = {
    def code =
      s"""${header(src.destPackageName)}
         |
         |import wvlet.airframe.http._
         |
         |${cls}""".stripMargin

    def cls: String =
      s"""class ${src.classDef.clsName}[Req, Resp](private val client: HttpSyncClient[Req, Resp]) extends AutoCloseable {
         |  override def close(): Unit = { client.close() }
         |  def getClient: HttpSyncClient[Req, Resp] = client
         |${indent(clsBody)}
         |}
         |""".stripMargin

    def clsBody: String = {
      generateNestedStub(src) { svc =>
        s"""object ${svc.serviceName} {
           |${indent(serviceBody(svc))}
           |}""".stripMargin
      }
    }

    def serviceBody(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters
              .map(x => s"${x.name}: ${x.surface.fullTypeName}") ++ Seq("requestFilter: Req => Req = identity")

          val sendRequestArgs = Seq.newBuilder[String]
          sendRequestArgs += s"""resourcePath = s"${m.path}""""
          sendRequestArgs ++= m.clientCallParameters
          sendRequestArgs += "requestFilter = requestFilter"

          val lines = Seq.newBuilder[String]
          m.requestModelClassDef.foreach { x =>
            lines += x.code()
          }
          lines += s"def ${m.name}(${inputArgs.mkString(", ")}): ${m.returnType.fullTypeName} = {"
          lines += s"  client.${m.clientMethodName}[${m.typeArgString}](${sendRequestArgs.result().mkString(", ")})"
          lines += s"}"
          lines.result().mkString("\n")
        }
        .mkString("\n")
    }

    code

  }
}

/**
  */
object ScalaJSClientGenerator extends HttpClientGenerator {

  import HttpClientGenerator._

  override def name: String             = "scalajs"
  override def defaultClassName: String = "ServiceJSClient"
  override def generate(src: ClientSourceDef): String = {
    def code: String =
      s"""${header(src.destPackageName)}
         |
         |import scala.concurrent.Future
         |import wvlet.airframe.surface.Surface
         |import wvlet.airframe.http.js.JSHttpClient
         |import wvlet.airframe.http.HttpMessage.Request
         |import wvlet.airframe.rx.RxStream
         |
         |${jsClientCls}
         |
         |${rpcClientCls}""".stripMargin

    def jsClientCls: String =
      s"""class ${src.classDef.clsName}(private val client: JSHttpClient = JSHttpClient.defaultClient) {
         |  def getClient: JSHttpClient = client
         |
         |${indent(clsBody)}
         |}""".stripMargin

    def clsBody: String = {
      generateNestedStub(src) { svc =>
        s"""object ${svc.serviceName} {
           |${indent(serviceBodyForFuture(svc))}
           |}""".stripMargin
      }
    }

    def rpcClientCls: String =
      s"""class ${src.classDef.clsName}Rx(private val client: JSHttpClient = JSHttpClient.defaultClient) {
         |  def getClient: JSHttpClient = client
         |
         |  /**
         |    * Override this method to add a common error handling
         |    */
         |  protected def toRx[A](future:Future[A]): RxStream[A] = {
         |    client.config.rxConverter(future).asInstanceOf[RxStream[A]]
         |  }
         |
         |${indent(rpcClientBody)}
         |}""".stripMargin

    def rpcClientBody: String = {
      generateNestedStub(src) { svc =>
        s"""object ${svc.serviceName} {
           |${indent(serviceBodyForRPC(svc))}
           |}""".stripMargin
      }
    }

    def inputArgs(m: ClientMethodDef): String = {
      val args = m.inputParameters.map(x => s"${x.name}: ${x.surface.fullTypeName}") ++
        Seq("requestFilter: Request => Request = identity")
      args.mkString(", ")
    }

    def sendRequestArgs(m: ClientMethodDef): String = {
      val args = Seq.newBuilder[String]
      args += s"""resourcePath = s"${m.path}""""
      args ++= m.clientCallParameters
      args ++= m.typeArgs.map(s => s"Surface.of[${s.fullTypeName}]")
      args += "requestFilter = requestFilter"
      args.result().mkString(", ")
    }

    def serviceBodyForFuture(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val lines = Seq.newBuilder[String]
          m.requestModelClassDef.foreach { x =>
            lines += x.code()
          }
          lines += s"def ${m.name}(${inputArgs(m)}): Future[${m.returnType.fullTypeName}] = {"
          lines += s"  client.${m.clientMethodName}[${m.typeArgString}](${sendRequestArgs(m)})"
          lines += s"}"
          lines.result().mkString("\n")
        }
        .mkString("\n")
    }

    def serviceBodyForRPC(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val lines = Seq.newBuilder[String]
          m.requestModelClassDef.foreach { x =>
            lines += x.code()
          }
          lines += s"def ${m.name}(${inputArgs(m)}): RxStream[${m.returnType.fullTypeName}] = {"
          lines += s"  toRx(client.${m.clientMethodName}[${m.typeArgString}](${sendRequestArgs(m)}))"
          lines += s"}"
          lines.result().mkString("\n")
        }
        .mkString("\n")
    }

    code
  }
}
