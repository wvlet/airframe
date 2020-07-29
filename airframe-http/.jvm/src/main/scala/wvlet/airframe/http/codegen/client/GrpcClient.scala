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
import wvlet.airframe.http.codegen.HttpClientIR.ClientServiceDef
import wvlet.airframe.http.codegen.client.ScalaHttpClient.{header, indent}

/**
  */
object GrpcSyncClient extends HttpClientType {

  override def name: String             = "grpc-sync"
  override def defaultFileName: String  = "ServiceGrpcSyncClient.scala"
  override def defaultClassName: String = "ServiceGrpcSyncClient"
  override def generate(src: HttpClientIR.ClientSourceDef): String = {
    def code =
      s"""${header(src.packageName)}
         |
         |import wvlet.airframe.http._
         |${src.importStatements}
         |
         |${cls}""".stripMargin

    def cls: String =
      s"""class ${src.classDef.clsName}(
         |  val channel: io.grpc.Channel,
         |  callOptions: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT,
         |  codecFactory: wvlet.airframe.codec.MessageCodecFactory = wvlet.airframe.codec.MessageCodecFactory.defaultFactoryForJSON
         |) extends io.grpc.stub.AbstractBlockingStub[${src.classDef.clsName}](channel, callOptions) with java.lang.AutoCloseable {
         |
         |  override def build(channel: io.grpc.Channel, callOptions: io.grpc.CallOptions): ${src.classDef.clsName} = {
         |    new ${src.classDef.clsName}(channel, callOptions)
         |  }
         |
         |  override def close(): Unit = {
         |    channel match {
         |      case m: io.grpc.ManagedChannel => m.shutdownNow()
         |      case _ =>
         |    }
         |  }
         |${indent(clsBody)}
         |}
         |""".stripMargin

    def clsBody: String = {
      src.classDef.services
        .map { svc =>
          s"""object ${svc.serviceName} {
             |  import io.grpc.stub.ClientCalls
             |  import wvlet.airframe.msgpack.spi.MsgPack
             |  import wvlet.airframe.codec.MessageCodec
             |  import wvlet.airframe.http.grpc.GrpcServiceBuilder.{RPCRequestMarshaller, RPCResponseMarshaller}
             |
             |${indent(methodDescriptors(svc))}
             |
             |${indent(serviceBody(svc))}
             |}""".stripMargin
        }.mkString("\n")
    }

    def methodDescriptors(svc: ClientServiceDef): String = {
      val md = svc.methods
        .map { m =>
          s"""private val ___${m.name}Descriptor: io.grpc.MethodDescriptor[MsgPack, Any] = {
             |  newBuilder("${src.packageName}.${svc.serviceName}/${m.name}")
             |    .setResponseMarshaller(new RPCResponseMarshaller[Any](
             |      codecFactory.of[${m.returnType.fullName.replaceAll("\\$", ".")}].asInstanceOf[MessageCodec[Any]]
             |    )).build()
             |}""".stripMargin
        }.mkString("\n")

      s"""private def newBuilder(fullMethodName:String): io.grpc.MethodDescriptor.Builder[MsgPack, Any] = {
         |  io.grpc.MethodDescriptor.newBuilder[MsgPack, Any]()
         |    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
         |    .setFullMethodName(fullMethodName)
         |    .setRequestMarshaller(RPCRequestMarshaller)
         |}
         |
         |${md}""".stripMargin
    }

    def serviceBody(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters.map(x => s"${x.name}: ${x.surface.name}")

          val requestObject = m.clientCallParameters.headOption.getOrElse("Map.empty")

          val lines = Seq.newBuilder[String]
          m.requestModelClassDef.foreach { x =>
            lines += x.code
          }
          lines += s"def ${m.name}(${inputArgs.mkString(", ")}): ${m.returnType} = {"
          lines += s"  val __m = ${requestObject}"
          lines += s"  val codec = codecFactory.of[${m.requestModelClassType}]"
          lines += s"  ClientCalls"
          lines += s"    .blockingUnaryCall(getChannel, ___${m.name}Descriptor, getCallOptions, codec.toMsgPack(__m))"
          lines += s"    .asInstanceOf[${m.returnType}]"
          lines += s"}"
          lines.result().mkString("\n")
        }.mkString("\n")
    }

    code
  }
}
