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
package wvlet.airframe.fluentd
import java.io.BufferedInputStream
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe._
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

case class MockFluentdConfig(port: Int)

trait MockFluentd extends LogSupport {
  lazy val socket = bind { config: MockFluentdConfig =>
    new ServerSocket(config.port)
  }

  val shutdown = new AtomicBoolean(false)

  val t = new Thread(new Runnable {
    override def run(): Unit = {
      val clientSocket = socket.accept()
      val out          = clientSocket.getOutputStream
      val in           = new BufferedInputStream(clientSocket.getInputStream)

      while (!shutdown.get()) {
        var b            = new Array[Byte](8192)
        var totalReadLen = 0
        var readLen      = in.read(b)
        while (readLen != -1) {
          val nextReadLen = in.read(b, totalReadLen, readLen)
          totalReadLen += readLen
          readLen = nextReadLen
        }
        if (totalReadLen > 0) {
          val v = ValueCodec.unpackMsgPack(b, 0, totalReadLen)
          logger.debug(s"Received event: ${v}")
        }
      }
    }
  })

  @PostConstruct
  def start: Unit = {
    t.start()
  }

  @PreDestroy
  def stop: Unit = {
    shutdown.set(true)
    socket.close()
    t.interrupt()
  }

}

trait MetricLoggingService extends MockFluentd {
  val factory = bind[MetricLoggerFactory]
}

case class FluencyMetric(id: Int, name: String) extends TaggedMetric {
  def metricTag = "fluency_metric"
}

/**
  *
  */
class FluencyTest extends AirSpec {
  val fluentdPort = IOUtil.randomPort

  val d = fluentd
    .withFluentdLogger(port = fluentdPort,
                       // Do not send ack for simplicity
                       ackResponseMode = false)
    .bind[MockFluentdConfig].toInstance(new MockFluentdConfig(fluentdPort))
    .bind[MockFluentd].toEagerSingleton
    .noLifeCycleLogging

  def `should send metrics to fluentd through Fluency`: Unit = {
    d.build[MetricLoggingService] { f =>
      // Use a regular emit method
      f.factory.getLogger.emit("mytag", Map("data" -> "hello"))

      // Use object metric logger
      val l = f.factory.getTypedLogger[FluencyMetric]
      l.emit(FluencyMetric(1, "leo"))
    }
  }
}
