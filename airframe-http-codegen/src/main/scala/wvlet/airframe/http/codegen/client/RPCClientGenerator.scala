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
import wvlet.airframe.http.{Http, HttpMethod}
import wvlet.airframe.http.codegen.HttpClientIR
import wvlet.airframe.http.codegen.HttpClientIR.{ClientMethodDef, ClientServiceDef}
import wvlet.airframe.http.codegen.client.HttpClientGenerator.RichSurface

/**
  * The default RPC client generator using Http.client.Sync/AsyncClient
  */
object RPCClientGenerator extends HttpClientGenerator {
  import ScalaHttpClientGenerator._

  override def name: String = "rpc"

  override def defaultClassName: String = "ServiceRPC"

  override def generate(src: HttpClientIR.ClientSourceDef): String = {
    def code: String =
      s"""${header(src.destPackageName)}
         |
         |import wvlet.airframe.http._
         |import wvlet.airframe.http.client.{HttpClientConfig, SyncClient, AsyncClient}
         |import wvlet.airframe.surface.Surface
         |import wvlet.airframe.rx.Rx
         |
         |${obj}""".stripMargin

    def obj: String =
      s"""object ${src.classDef.clsName} {
         |${indent(factoryMethods)}
         |
         |${indent(internalDefs)}
         |${indent(syncClientClass)}
         |${indent(asyncClientClass)}
         |}
         |""".stripMargin

    def factoryMethods: String =
      s"""def newRPCSyncClient(client: SyncClient): RPCSyncClient = new RPCSyncClient(client)
         |def newRPCAsyncClient(client: AsyncClient): RPCAsyncClient = new RPCAsyncClient(client)
         |""".stripMargin

    def internalDefs: String = {
      s"""object internal {
         |${indent(src.classDef.services.map(internalDefOf(_)).mkString("\n"))}
         |}""".stripMargin
    }

    def internalDefOf(svc: ClientServiceDef): String = {
      s"""object ${svc.serviceName}Internals {
         |${indent(modelClasses(svc))}
         |
         |${indent(rpcMethodDefs(svc))}
         |}""".stripMargin
    }

    def rpcMethodDefs(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          s"""lazy val __m_${m.name} = RPCMethod("${m.path}", "${svc.interfaceName}", "${m.name}", Surface.of[${m.requestModelClassType}], Surface.of[${m.returnType.fullTypeName}])"""
        }.mkString("\n")
    }

    // Generate model classes that wrap request parameters
    def modelClasses(svc: ClientServiceDef): String = {
      svc.methods
        .filter { x =>
          x.requestModelClassDef.isDefined
        }
        .map(_.requestModelClassDef.get.code(isPrivate = false))
        .mkString("\n")
    }

    def syncClientClass: String =
      s"""class RPCSyncClient(client:SyncClient) extends wvlet.airframe.http.client.HttpClientFactory[RPCSyncClient] with AutoCloseable {
         |  override protected def build(newConfig: HttpClientConfig): RPCSyncClient = {
         |    new RPCSyncClient(client.withConfig(_ => newConfig))
         |  }
         |  override protected def config: HttpClientConfig = client.config
         |  override def close(): Unit = { client.close() }
         |  def getClient: SyncClient = client
         |
         |${indent(syncClientBody)}
         |}
         |""".stripMargin

    def asyncClientClass: String =
      s"""class RPCAsyncClient(client:AsyncClient) extends wvlet.airframe.http.client.HttpClientFactory[RPCAsyncClient] with AutoCloseable {
         |  override protected def build(newConfig: HttpClientConfig): RPCAsyncClient = {
         |    new RPCAsyncClient(client.withConfig(_ => newConfig))
         |  }
         |  override protected def config: HttpClientConfig = client.config
         |  override def close(): Unit = { client.close() }
         |  def getClient: AsyncClient = client
         |${indent(asyncClientBody)}
         |}
         |""".stripMargin

    def syncClientBody: String = {
      HttpClientGenerator.generateNestedStub(src) { svc =>
        s"""object ${svc.serviceName} {
           |  import internal.${svc.serviceName}Internals._
           |${indent(rpcMethods(svc, isAsync = false))}
           |}""".stripMargin
      }
    }

    def asyncClientBody: String = {
      HttpClientGenerator.generateNestedStub(src) { svc =>
        s"""object ${svc.serviceName} {
           |  import internal.${svc.serviceName}Internals._
           |${indent(rpcMethods(svc, isAsync = true))}
           |}""".stripMargin
      }
    }

    def sendRequestArgs(m: ClientMethodDef): String = {
      val b = Seq.newBuilder[String]
      b += s"__m_${m.name}"
      b ++= m.clientCallParameters
      b.result().mkString(", ")
    }

    def rpcMethods(svc: ClientServiceDef, isAsync: Boolean): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters
              .map(x => s"${x.name}: ${x.surface.fullTypeName}")

          val returnType = if (isAsync) s"Rx[${m.returnType.fullTypeName}]" else m.returnType.fullTypeName
          if (m.isRPC) {
            s"""def ${m.name}(${inputArgs.mkString(", ")}): ${returnType} = {
               |  client.rpc[${m.typeArgString}](${sendRequestArgs(m)})
               |}""".stripMargin
          } else {
            // For @Endpoint calls
            m.httpMethod match {
              case HttpMethod.GET =>
                s"""def ${m.name}(${inputArgs.mkString(", ")}): ${returnType} = {
                   |  client.readAs[${m.returnType.fullTypeName}](Http.GET(s"${m.path}"))
                   |}""".stripMargin
              case _ =>
                val args = Seq.newBuilder[String]
                args += s"""Http.${m.httpMethod}(s"${m.path}")"""
                args ++= m.clientCallParameters
                s"""def ${m.name}(${inputArgs.mkString(", ")}): ${returnType} = {
                   |  client.call[${m.typeArgString}](${args.result().mkString(", ")})
                   |}""".stripMargin
            }
          }
        }
        .mkString("\n")
    }

    code
  }
}
