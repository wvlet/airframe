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
  * Generate gRPC client stubs
  */
object GrpcClient extends HttpClientType {

  override def name: String             = "grpc"
  override def defaultFileName: String  = "ServiceGrpcClient.scala"
  override def defaultClassName: String = "ServiceGrpcClient"

  override def generate(src: HttpClientIR.ClientSourceDef): String = {
    def code =
      s"""${header(src.packageName)}
         |
         |import wvlet.airframe.http._
         |${src.importStatements}
         |
         |${companionObject}
         |""".stripMargin

    def companionObject: String =
      s"""object ${src.classDef.clsName} {
         |  import wvlet.airframe.msgpack.spi.MsgPack
         |  import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
         |  import wvlet.airframe.http.grpc.GrpcServiceBuilder.{RPCRequestMarshaller, RPCResponseMarshaller}
         |
         |${indent(descriptorBuilder)}
         |
         |${indent(descriptorBody)}
         |
         |${indent(cls)}
         |}""".stripMargin

    def descriptorBuilder: String = {
      s"""private def newDescriptorBuilder(fullMethodName:String) : io.grpc.MethodDescriptor.Builder[MsgPack, Any] = {
         |  io.grpc.MethodDescriptor.newBuilder[MsgPack, Any]()
         |    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
         |    .setFullMethodName(fullMethodName)
         |    .setRequestMarshaller(RPCRequestMarshaller)
         |}""".stripMargin
    }

    def descriptorBody: String = {
      src.classDef.services
        .map { svc =>
          s"""class ${svc.serviceName}Descriptors(codecFactory: MessageCodecFactory) {
             |${indent(methodDescriptors(svc))}
             |}""".stripMargin
        }.mkString("\n")
    }

    def methodDescriptors(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          s"""val ___${m.name}Descriptor: io.grpc.MethodDescriptor[MsgPack, Any] = {
             |  newDescriptorBuilder("${src.packageName}.${svc.serviceName}/${m.name}")
             |    .setResponseMarshaller(new RPCResponseMarshaller[Any](
             |      codecFactory.of[${m.returnType.fullName.replaceAll("\\$", ".")}].asInstanceOf[MessageCodec[Any]]
             |    )).build()
             |}""".stripMargin
        }.mkString("\n")
    }

    def cls: String =
      s"""def newSyncClient(
         |  channel: io.grpc.Channel,
         |  callOptions: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT,
         |  codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
         |): SyncClient = new SyncClient(channel, callOptions, codecFactory)
         |
         |class SyncClient(
         |  val channel: io.grpc.Channel,
         |  callOptions: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT,
         |  codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
         |) extends io.grpc.stub.AbstractBlockingStub[SyncClient](channel, callOptions) with java.lang.AutoCloseable {
         |
         |  override def build(channel: io.grpc.Channel, callOptions: io.grpc.CallOptions): SyncClient = {
         |    new SyncClient(channel, callOptions, codecFactory)
         |  }
         |
         |  override def close(): Unit = {
         |    channel match {
         |      case m: io.grpc.ManagedChannel => m.shutdownNow()
         |      case _ =>
         |    }
         |  }
         |
         |${indent(clientStub)}
         |}
         |""".stripMargin

    def clientStub: String = {
      src.classDef.services
        .map { svc =>
          s"""object ${svc.serviceName} {
             |  import io.grpc.stub.ClientCalls
             |
             |  private val descriptors = new ${svc.serviceName}Descriptors(codecFactory)
             |  import descriptors._
             |
             |${indent(clientBody(svc))}
             |}""".stripMargin
        }.mkString("\n")
    }

    def clientBody(svc: ClientServiceDef): String = {
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
