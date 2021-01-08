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
import wvlet.airframe.http.codegen.HttpClientIR.{
  ClientServiceDef,
  GrpcMethodType
}
import wvlet.airframe.http.codegen.client.ScalaHttpClientGenerator.{
  header,
  indent
}

/**
  * Generate gRPC client stubs
  */
object GrpcClientGenerator extends HttpClientGenerator {

  override def name: String = "grpc"
  override def defaultFileName: String = "ServiceGrpc.scala"
  override def defaultClassName: String = "ServiceGrpc"

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
         |${indent(modelClasses)}
         |
         |${indent(syncClientClass)}
         |
         |${indent(asyncClientClass)}
         |}""".stripMargin

    def descriptorBuilder: String = {
      s"""private def newDescriptorBuilder(
         |  fullMethodName:String,
         |  methodType:io.grpc.MethodDescriptor.MethodType
         |) : io.grpc.MethodDescriptor.Builder[MsgPack, Any] = {
         |  io.grpc.MethodDescriptor.newBuilder[MsgPack, Any]()
         |    .setType(methodType)
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
        }
        .mkString("\n")
    }

    def methodDescriptors(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          s"""val ${m.name}Descriptor: io.grpc.MethodDescriptor[MsgPack, Any] = {
             |  newDescriptorBuilder("${src.packageName}.${svc.serviceName}/${m.name}", ${m.grpcMethodType.code})
             |    .setResponseMarshaller(new RPCResponseMarshaller[Any](
             |      codecFactory.of[${m.grpcReturnType.fullName.replaceAll(
               "\\$",
               ".")}].asInstanceOf[MessageCodec[Any]]
             |    )).build()
             |}""".stripMargin
        }
        .mkString("\n")
    }

    def modelClasses: String = {
      src.classDef.services
        .map { svc =>
          s"""object ${svc.serviceName}Models {
           |${indent(
               svc.methods
                 .filter { x =>
                   x.requestModelClassDef.isDefined
                 }
                 .map(_.requestModelClassDef.get.code(isPrivate = false))
                 .mkString("\n")
             )}
           |}""".stripMargin
        }
        .mkString("\n")
    }

    def syncClientClass: String =
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
         |  override protected def build(channel: io.grpc.Channel, callOptions: io.grpc.CallOptions): SyncClient = {
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
         |${indent(syncClientStub)}
         |}
         |""".stripMargin

    def syncClientStub: String = {
      src.classDef.services
        .map { svc =>
          s"""object ${svc.serviceName} {
             |  private val descriptors = new ${svc.serviceName}Descriptors(codecFactory)
             |
             |  import io.grpc.stub.ClientCalls
             |  import ${svc.serviceName}Models._
             |
             |${indent(syncClientBody(svc))}
             |}""".stripMargin
        }
        .mkString("\n")
    }

    def syncClientBody(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters.map(x => s"${x.name}: ${x.surface.name}")

          val requestObject = m.clientCallParameters.headOption.getOrElse(
            "Map.empty[String, Any]")
          val lines = Seq.newBuilder[String]
          lines += s"def ${m.name}(${inputArgs.mkString(", ")}): ${m.returnType} = {"
          m.grpcMethodType match {
            case GrpcMethodType.UNARY =>
              lines += s"  val __m = ${requestObject}"
              lines += s"  val codec = codecFactory.of[${m.requestModelClassType}]"
              lines += s"  ClientCalls"
              lines += s"    .blockingUnaryCall(getChannel, descriptors.${m.name}Descriptor, getCallOptions, codec.toMsgPack(__m))"
              lines += s"    .asInstanceOf[${m.returnType}]"
            case GrpcMethodType.SERVER_STREAMING =>
              lines += s"  val __m = ${requestObject}"
              lines += s"  val codec = codecFactory.of[${m.requestModelClassType}]"
              lines += s"  val responseObserver = wvlet.airframe.http.grpc.GrpcClientCalls.blockingResponseObserver[${m.grpcReturnType}]"
              lines += s"  ClientCalls"
              lines += s"    .asyncServerStreamingCall("
              lines += s"       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),"
              lines += s"       codec.toMsgPack(__m),"
              lines += s"       responseObserver"
              lines += s"    )"
              lines += s"  responseObserver.toRx"
            case GrpcMethodType.CLIENT_STREAMING =>
              lines += s"  val responseObserver = wvlet.airframe.http.grpc.GrpcClientCalls.blockingResponseObserver[${m.grpcReturnType}]"
              lines += s"  val requestObserver = ClientCalls"
              lines += s"    .asyncClientStreamingCall("
              lines += s"       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),"
              lines += s"       responseObserver"
              lines += s"    )"
              lines += s"  wvlet.airframe.http.grpc.GrpcClientCalls.readClientRequestStream("
              lines += s"    ${m.inputParameters.head.name},"
              lines += s"    codecFactory.of[${m.grpcClientStreamingRequestType}],"
              lines += s"    requestObserver"
              lines += s"  )"
              lines += s"  responseObserver.toRx.toSeq.head"
            case GrpcMethodType.BIDI_STREAMING =>
              lines += s"  val responseObserver = wvlet.airframe.http.grpc.GrpcClientCalls.blockingResponseObserver[${m.grpcReturnType}]"
              lines += s"  val requestObserver = ClientCalls"
              lines += s"    .asyncBidiStreamingCall("
              lines += s"       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),"
              lines += s"       responseObserver"
              lines += s"    )"
              lines += s"  wvlet.airframe.http.grpc.GrpcClientCalls.readClientRequestStream("
              lines += s"    ${m.inputParameters.head.name},"
              lines += s"    codecFactory.of[${m.grpcClientStreamingRequestType}],"
              lines += s"    requestObserver"
              lines += s"  )"
              lines += s"  responseObserver.toRx"
          }
          lines += s"}"
          lines.result().mkString("\n")
        }
        .mkString("\n")
    }

    def asyncClientClass: String =
      s"""def newAsyncClient(
         |  channel: io.grpc.Channel,
         |  callOptions: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT,
         |  codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
         |): AsyncClient = new AsyncClient(channel, callOptions, codecFactory)
         |
         |class AsyncClient(
         |  val channel: io.grpc.Channel,
         |  callOptions: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT,
         |  codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
         |) extends io.grpc.stub.AbstractAsyncStub[AsyncClient](channel, callOptions) with java.lang.AutoCloseable {
         |
         |  override protected def build(channel: io.grpc.Channel, callOptions: io.grpc.CallOptions): AsyncClient = {
         |    new AsyncClient(channel, callOptions, codecFactory)
         |  }
         |
         |  override def close(): Unit = {
         |    channel match {
         |      case m: io.grpc.ManagedChannel => m.shutdownNow()
         |      case _ =>
         |    }
         |  }
         |
         |${indent(asyncClientStub)}
         |}
         |
         |""".stripMargin

    def asyncClientStub: String = {
      src.classDef.services
        .map { svc =>
          s"""object ${svc.serviceName} {
             |  private val descriptors = new ${svc.serviceName}Descriptors(codecFactory)
             |
             |  import io.grpc.stub.ClientCalls
             |  import ${svc.serviceName}Models._
             |
             |${indent(asyncClientBody(svc))}
             |}""".stripMargin
        }
        .mkString("\n")
    }

    def asyncClientBody(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters.map(x => s"${x.name}: ${x.surface.name}")

          val requestObject =
            m.clientCallParameters.headOption.getOrElse("Map.empty")
          val clientArgs = inputArgs :+ s"responseObserver: io.grpc.stub.StreamObserver[${m.grpcReturnType}]"
          val lines = m.grpcMethodType match {
            case GrpcMethodType.UNARY =>
              s"""def ${m.name}(${clientArgs.mkString(", ")}): Unit = {
                 |  val __m = ${requestObject}
                 |  val codec = codecFactory.of[${m.requestModelClassType}]
                 |  ClientCalls
                 |    .asyncUnaryCall[MsgPack, Any](
                 |       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),
                 |       codec.toMsgPack(__m),
                 |       responseObserver.asInstanceOf[io.grpc.stub.StreamObserver[Any]]
                 |    )
                 |}""".stripMargin
            case GrpcMethodType.SERVER_STREAMING =>
              s"""def ${m.name}(${clientArgs.mkString(", ")}): Unit = {
                 |  val __m = ${requestObject}
                 |  val codec = codecFactory.of[${m.requestModelClassType}]
                 |  ClientCalls
                 |    .asyncServerStreamingCall[MsgPack, Any](
                 |       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),
                 |       codec.toMsgPack(__m),
                 |       responseObserver.asInstanceOf[io.grpc.stub.StreamObserver[Any]]
                 |    )
                 |}""".stripMargin
            case GrpcMethodType.CLIENT_STREAMING =>
              s"""def ${m.name}(responseObserver: io.grpc.stub.StreamObserver[${m.grpcReturnType}])
                 |  : io.grpc.stub.StreamObserver[${m.grpcClientStreamingRequestType}] = {
                 |  val codec = codecFactory.of[${m.grpcClientStreamingRequestType}]
                 |  val requestObserver = ClientCalls
                 |    .asyncClientStreamingCall[MsgPack, Any](
                 |       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),
                 |       responseObserver.asInstanceOf[io.grpc.stub.StreamObserver[Any]]
                 |    )
                 |  wvlet.airframe.http.grpc.GrpcClientCalls.translate[MsgPack, ${m.grpcClientStreamingRequestType}](
                 |    requestObserver,
                 |    codec.toMsgPack(_)
                 |  )
                 |}""".stripMargin
            case GrpcMethodType.BIDI_STREAMING =>
              s"""def ${m.name}(responseObserver: io.grpc.stub.StreamObserver[${m.grpcReturnType}])
                 |  : io.grpc.stub.StreamObserver[${m.grpcClientStreamingRequestType}] = {
                 |  val codec = codecFactory.of[${m.grpcClientStreamingRequestType}]
                 |  val requestObserver = ClientCalls
                 |    .asyncBidiStreamingCall[MsgPack, Any](
                 |       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),
                 |       responseObserver.asInstanceOf[io.grpc.stub.StreamObserver[Any]]
                 |    )
                 |  wvlet.airframe.http.grpc.GrpcClientCalls.translate[MsgPack, ${m.grpcClientStreamingRequestType}](
                 |    requestObserver,
                 |    codec.toMsgPack(_)
                 |  )
                 |}""".stripMargin
          }
          lines
        }
        .mkString("\n")
    }

    code
  }
}
