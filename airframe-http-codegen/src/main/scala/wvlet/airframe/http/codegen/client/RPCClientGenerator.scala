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
         |import wvlet.airframe.http.HttpMessage.Request
         |import wvlet.airframe.http.client.{SyncClient, AsyncClient}
         |import scala.concurrent.Future
         |
         |${obj}""".stripMargin

    def obj: String =
      s"""object ${src.classDef.clsName} {
         |${indent(factoryMethods)}
         |
         |${indent(syncClientClass)}
         |${indent(asyncClientClass)}
         |}
         |""".stripMargin

    def factoryMethods: String =
      s"""def newRPCSyncClient(client: SyncClient): RPCSyncClient = new RPCSyncClient(client)
         |def newRPCAsyncClient(client: AsyncClient): RPCAsyncClient = new RPCAsyncClient(client)
         |""".stripMargin

    def syncClientClass: String =
      s"""class RPCSyncClient(private val client:SyncClient) extends AutoCloseable {
         |  override def close(): Unit = { client.close() }
         |  def getClient: SyncClient = client
         |
         |${indent(syncClientBody)}
         |}
         |""".stripMargin

    def asyncClientClass: String =
      s"""class RPCAsyncClient(private val client:AsyncClient) extends AutoCloseable {
         |  override def close(): Unit = { client.close() }
         |  def getClient: AsyncClient = client
         |
         |${indent(asyncClientBody)}
         |}
         |""".stripMargin

    def syncClientBody: String = {
      HttpClientGenerator.generateNestedStub(src) { svc =>
        s"""object ${svc.serviceName} {
           |${indent(syncClientMethods(svc))}
           |}""".stripMargin
      }
    }

    def asyncClientBody: String = {
      HttpClientGenerator.generateNestedStub(src) { svc =>
        s"""object ${svc.serviceName} {
           |${indent(asyncClientMethods(svc))}
           |}""".stripMargin
      }
    }
    def modelClassDef(m: ClientMethodDef): String = {
      val modelClassDefs = Seq.newBuilder[String]
      m.requestModelClassDef.foreach { x =>
        modelClassDefs += x.code()
      }
      modelClassDefs.result().mkString("\n")
    }

    def sendRequestArgs(m: ClientMethodDef): String = {
      Seq(
        s"\"${m.path}\"",
        m.clientCallParameters.mkString(", "),
        "requestFilter"
      ).mkString(", ")
    }

    def syncClientMethods(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters
              .map(x => s"${x.name}: ${x.surface.fullTypeName}") ++ Seq("requestFilter: Request => Request = identity")

          s"""${modelClassDef(m)}
             |def ${m.name}(${inputArgs.mkString(", ")}): ${m.returnType.fullTypeName} = {
             |  client.rpc[${m.typeArgString}](${sendRequestArgs(m)})
             |}""".stripMargin
        }
        .mkString("\n")
    }

    def asyncClientMethods(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters
              .map(x => s"${x.name}: ${x.surface.fullTypeName}") ++ Seq("requestFilter: Request => Request = identity")

          s"""${modelClassDef(m)}
             |def ${m.name}(${inputArgs.mkString(", ")}): Future[${m.returnType.fullTypeName}] = {
             |  client.rpc[${m.typeArgString}](${sendRequestArgs(m)})
             |}""".stripMargin
        }
        .mkString("\n")
    }

    code
  }
}
