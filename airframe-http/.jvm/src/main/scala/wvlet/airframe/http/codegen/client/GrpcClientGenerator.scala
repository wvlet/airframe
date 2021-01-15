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
  ClientServicePackages,
  ClientSourceDef,
  GrpcMethodType
}
import wvlet.airframe.http.codegen.client.ScalaHttpClientGenerator.{
  header,
  indent
}
import wvlet.airframe.surface.{MethodParameter, Surface}
import wvlet.log.LogSupport

/**
  * Generate gRPC client stubs
  */
object GrpcClientGenerator extends HttpClientGenerator with LogSupport {

  override def name: String = "grpc"
  override def defaultFileName: String = "ServiceGrpc.scala"
  override def defaultClassName: String = "ServiceGrpc"

  private implicit class RichSurface(val s: Surface) extends AnyVal {
    def fullTypeName: String = s.fullName.replaceAll("\\$", ".")
  }

  override def generate(src: HttpClientIR.ClientSourceDef): String = {
    def code =
      s"""${header(src.destPackageName)}
         |
         |import wvlet.airframe.http._
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
         |${indent(descriptorModules, 1)}
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

    def descriptorModules: String = {
      def descriptorBody(svc: ClientServiceDef): String = {
        s"""class ${svc.serviceName}Descriptors(codecFactory: MessageCodecFactory) {
           |${indent(methodDescriptors(svc))}
           |}""".stripMargin
      }

      def methodDescriptors(svc: ClientServiceDef): String = {
        svc.methods
          .map { m =>
            s"""val ${m.name}Descriptor: io.grpc.MethodDescriptor[MsgPack, Any] = {
               |  newDescriptorBuilder("${svc.fullPackageName}.${svc.serviceName}/${m.name}", ${m.grpcMethodType.code})
               |    .setResponseMarshaller(new RPCResponseMarshaller[Any](
               |      codecFactory.of[${m.grpcReturnType.fullTypeName}].asInstanceOf[MessageCodec[Any]]
               |    )).build()
               |}""".stripMargin
          }
          .mkString("\n")
      }

      def modelClasses(svc: ClientServiceDef): String = {
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

      def traverse(current: ClientServicePackages): String = {
        val serviceBody = current.services.map(descriptorBody(_)).mkString("\n")
        val modelClassesBody =
          current.services.map(modelClasses(_)).mkString("\n")
        val body =
          s"""${serviceBody}
             |${modelClassesBody}
             |${current.children
               .map(traverse(_))
               .mkString("\n")}""".stripMargin.trim
        if (current.packageLeafName.isEmpty) {
          body
        } else {
          s"""object ${current.packageLeafName} {
             |${indent(body)}
             |}""".stripMargin
        }
      }

      s"""object internal {
         |${indent(traverse(src.classDef.toNestedPackages))}
         |}
         |""".stripMargin
    }

    def generateStub(s: ClientSourceDef)(
        stubBodyGenerator: ClientServiceDef => String): String = {
      def serviceStub(svc: ClientServiceDef): String = {
        s"""object ${svc.serviceName} {
           |  private val descriptors = new ${svc.fullServiceName}Descriptors(codecFactory)
           |
           |  import io.grpc.stub.ClientCalls
           |  import ${svc.fullServiceName}Models._
           |
           |${indent(stubBodyGenerator(svc))}
           |}""".stripMargin
      }

      // Traverse nested packages
      def traverse(p: ClientServicePackages): String = {
        val serviceStubBody =
          p.services.map(svc => serviceStub(svc)).mkString("\n")
        val childServiceStubBody = p.children.map(traverse(_)).mkString("\n")

        val body =
          s"""${serviceStubBody}
             |${childServiceStubBody}""".stripMargin.trim
        if (p.packageLeafName.isEmpty) {
          body
        } else {
          s"""object ${p.packageLeafName} {
             |${indent(body)}
             |}""".stripMargin
        }
      }

      traverse(s.classDef.toNestedPackages)
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

    def syncClientStub: String =
      generateStub(src)(syncClientBody)

    def inputParameterArg(p: MethodParameter): String = {
      s"${p.name}: ${p.surface.fullTypeName}"
    }

    def syncClientBody(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters.map(inputParameterArg)

          val requestObject = m.clientCallParameters.headOption.getOrElse(
            "Map.empty[String, Any]")
          val lines = Seq.newBuilder[String]
          lines += s"def ${m.name}(${inputArgs.mkString(", ")}): ${m.returnType.fullTypeName} = {"
          m.grpcMethodType match {
            case GrpcMethodType.UNARY =>
              lines += s"  val __m = ${requestObject}"
              lines += s"  val codec = codecFactory.of[${m.requestModelClassType}]"
              lines += s"  ClientCalls"
              lines += s"    .blockingUnaryCall(getChannel, descriptors.${m.name}Descriptor, getCallOptions, codec.toMsgPack(__m))"
              lines += s"    .asInstanceOf[${m.returnType.fullTypeName}]"
            case GrpcMethodType.SERVER_STREAMING =>
              lines += s"  val __m = ${requestObject}"
              lines += s"  val codec = codecFactory.of[${m.requestModelClassType}]"
              lines += s"  val responseObserver = wvlet.airframe.http.grpc.GrpcClientCalls.blockingResponseObserver[${m.grpcReturnType.fullTypeName}]"
              lines += s"  ClientCalls"
              lines += s"    .asyncServerStreamingCall("
              lines += s"       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),"
              lines += s"       codec.toMsgPack(__m),"
              lines += s"       responseObserver"
              lines += s"    )"
              lines += s"  responseObserver.toRx"
            case GrpcMethodType.CLIENT_STREAMING =>
              lines += s"  val responseObserver = wvlet.airframe.http.grpc.GrpcClientCalls.blockingResponseObserver[${m.grpcReturnType.fullTypeName}]"
              lines += s"  val requestObserver = ClientCalls"
              lines += s"    .asyncClientStreamingCall("
              lines += s"       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),"
              lines += s"       responseObserver"
              lines += s"    )"
              lines += s"  wvlet.airframe.http.grpc.GrpcClientCalls.readClientRequestStream("
              lines += s"    ${m.inputParameters.head.name},"
              lines += s"    codecFactory.of[${m.grpcClientStreamingRequestType.fullTypeName}],"
              lines += s"    requestObserver"
              lines += s"  )"
              lines += s"  responseObserver.toRx.toSeq.head"
            case GrpcMethodType.BIDI_STREAMING =>
              lines += s"  val responseObserver = wvlet.airframe.http.grpc.GrpcClientCalls.blockingResponseObserver[${m.grpcReturnType.fullTypeName}]"
              lines += s"  val requestObserver = ClientCalls"
              lines += s"    .asyncBidiStreamingCall("
              lines += s"       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),"
              lines += s"       responseObserver"
              lines += s"    )"
              lines += s"  wvlet.airframe.http.grpc.GrpcClientCalls.readClientRequestStream("
              lines += s"    ${m.inputParameters.head.name},"
              lines += s"    codecFactory.of[${m.grpcClientStreamingRequestType.fullTypeName}],"
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

    def asyncClientStub: String = generateStub(src)(asyncClientBody)

    def asyncClientBody(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val inputArgs =
            m.inputParameters.map(inputParameterArg)

          val requestObject =
            m.clientCallParameters.headOption.getOrElse(
              "Map.empty[String, Any]")
          val clientArgs =
            inputArgs :+ s"responseObserver: io.grpc.stub.StreamObserver[${m.grpcReturnType.fullTypeName}]"
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
              s"""def ${m.name}(responseObserver: io.grpc.stub.StreamObserver[${m.grpcReturnType.fullTypeName}])
                 |  : io.grpc.stub.StreamObserver[${m.grpcClientStreamingRequestType.fullTypeName}] = {
                 |  val codec = codecFactory.of[${m.grpcClientStreamingRequestType.fullTypeName}]
                 |  val requestObserver = ClientCalls
                 |    .asyncClientStreamingCall[MsgPack, Any](
                 |       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),
                 |       responseObserver.asInstanceOf[io.grpc.stub.StreamObserver[Any]]
                 |    )
                 |  wvlet.airframe.http.grpc.GrpcClientCalls.translate[MsgPack, ${m.grpcClientStreamingRequestType.fullTypeName}](
                 |    requestObserver,
                 |    codec.toMsgPack(_)
                 |  )
                 |}""".stripMargin
            case GrpcMethodType.BIDI_STREAMING =>
              s"""def ${m.name}(responseObserver: io.grpc.stub.StreamObserver[${m.grpcReturnType.fullTypeName}])
                 |  : io.grpc.stub.StreamObserver[${m.grpcClientStreamingRequestType.fullTypeName}] = {
                 |  val codec = codecFactory.of[${m.grpcClientStreamingRequestType.fullTypeName}]
                 |  val requestObserver = ClientCalls
                 |    .asyncBidiStreamingCall[MsgPack, Any](
                 |       getChannel.newCall(descriptors.${m.name}Descriptor, getCallOptions),
                 |       responseObserver.asInstanceOf[io.grpc.stub.StreamObserver[Any]]
                 |    )
                 |  wvlet.airframe.http.grpc.GrpcClientCalls.translate[MsgPack, ${m.grpcClientStreamingRequestType.fullTypeName}](
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
