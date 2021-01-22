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
package wvlet.airframe.benchmark.http

import scala.collection.immutable.Map

object ServiceGrpc {
  import wvlet.airframe.msgpack.spi.MsgPack
  import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
  import wvlet.airframe.http.grpc.internal.GrpcServiceBuilder.{GrpcRequestMarshaller, GrpcResponseMarshaller}

  private def newDescriptorBuilder(
      fullMethodName: String,
      methodType: io.grpc.MethodDescriptor.MethodType
  ): io.grpc.MethodDescriptor.Builder[MsgPack, Any] = {
    io.grpc.MethodDescriptor
      .newBuilder[MsgPack, Any]()
      .setType(methodType)
      .setFullMethodName(fullMethodName)
      .setRequestMarshaller(GrpcRequestMarshaller)
  }

  class GreeterDescriptors(codecFactory: MessageCodecFactory) {
    val helloDescriptor: io.grpc.MethodDescriptor[MsgPack, Any] = {
      newDescriptorBuilder("wvlet.airframe.benchmark.http.Greeter/hello", io.grpc.MethodDescriptor.MethodType.UNARY)
        .setResponseMarshaller(
          new GrpcResponseMarshaller[Any](
            codecFactory.of[java.lang.String].asInstanceOf[MessageCodec[Any]]
          )
        ).build()
    }
  }

  object GreeterModels {}

  def newSyncClient(
      channel: io.grpc.Channel,
      callOptions: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ): SyncClient = new SyncClient(channel, callOptions, codecFactory)

  class SyncClient(
      val channel: io.grpc.Channel,
      callOptions: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ) extends io.grpc.stub.AbstractBlockingStub[SyncClient](channel, callOptions)
      with java.lang.AutoCloseable {

    override protected def build(channel: io.grpc.Channel, callOptions: io.grpc.CallOptions): SyncClient = {
      new SyncClient(channel, callOptions, codecFactory)
    }

    override def close(): Unit = {
      channel match {
        case m: io.grpc.ManagedChannel => m.shutdownNow()
        case _                         =>
      }
    }

    object Greeter {
      private val descriptors = new GreeterDescriptors(codecFactory)

      import io.grpc.stub.ClientCalls
      import GreeterModels._

      def hello(name: String): String = {
        val __m   = Map("name" -> name)
        val codec = codecFactory.of[Map[String, Any]]
        ClientCalls
          .blockingUnaryCall(getChannel, descriptors.helloDescriptor, getCallOptions, codec.toMsgPack(__m))
          .asInstanceOf[String]
      }
    }
  }

  def newAsyncClient(
      channel: io.grpc.Channel,
      callOptions: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ): AsyncClient = new AsyncClient(channel, callOptions, codecFactory)

  class AsyncClient(
      val channel: io.grpc.Channel,
      callOptions: io.grpc.CallOptions = io.grpc.CallOptions.DEFAULT,
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ) extends io.grpc.stub.AbstractAsyncStub[AsyncClient](channel, callOptions)
      with java.lang.AutoCloseable {

    override protected def build(channel: io.grpc.Channel, callOptions: io.grpc.CallOptions): AsyncClient = {
      new AsyncClient(channel, callOptions, codecFactory)
    }

    override def close(): Unit = {
      channel match {
        case m: io.grpc.ManagedChannel => m.shutdownNow()
        case _                         =>
      }
    }

    object Greeter {
      private val descriptors = new GreeterDescriptors(codecFactory)

      import io.grpc.stub.ClientCalls
      import GreeterModels._

      def hello(name: String, responseObserver: io.grpc.stub.StreamObserver[String]): Unit = {
        val __m   = Map("name" -> name)
        val codec = codecFactory.of[Map[String, Any]]
        ClientCalls
          .asyncUnaryCall[MsgPack, Any](
            getChannel.newCall(descriptors.helloDescriptor, getCallOptions),
            codec.toMsgPack(__m),
            responseObserver.asInstanceOf[io.grpc.stub.StreamObserver[Any]]
          )
      }
    }
  }
}
