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
import org.komamitsu.fluency.ingester.sender.ErrorHandler
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

/**
  */
object FluentdTest extends AirSpec {

  test("create default Fluentd logger") {
    IOUtil.withResource(Fluentd.client.newFluentdLogger()) { l =>
      l.flush()
    }
  }

  test("configure Fluentd loggers") {
    var config = Fluentd.client

    config.tagPrefix shouldBe empty
    config = config.withTagPrefix("data")
    config.tagPrefix shouldBe Some("data")

    config.useExtendedEventTime shouldBe false
    config = config.withExtendedEventTime
    config.useExtendedEventTime shouldBe true
    config = config.noExtendedEventTime
    config.useExtendedEventTime shouldBe false

    val b = config.maxBufferSize
    config = config.withMaxBufferSize(b * 2)
    config.maxBufferSize shouldBe b * 2

    val f = config.flushIntervalMillis
    config = config.withFlushIntervalMillis(f * 2)
    config.flushIntervalMillis shouldBe f * 2

    val r = config.bufferChunkRetentionSize
    config = config.withBufferChunkRetentionSize(r * 2)
    config.bufferChunkRetentionSize shouldBe r * 2

    val m = config.bufferChunkRetentionTimeMillis
    config = config.withBufferChunkRetentionTimeMillis(m * 2)
    config.bufferChunkRetentionTimeMillis shouldBe m * 2

    config = config.withJVMHeapBuffer
    config.jvmHeapBufferMode shouldBe true
    config = config.noJVMHeapBuffer
    config.jvmHeapBufferMode shouldBe false

    config = config.withAckResponse
    config.ackResponseMode shouldBe true
    config = config.noAckResponse
    config.ackResponseMode shouldBe false

    config.sslEnabled shouldBe false
    config = config.withSSL
    config.sslEnabled shouldBe true
    config = config.noSSL
    config.sslEnabled shouldBe false

    config.fileBackupDir shouldBe null
    config = config.withFileBackupDir("dir")
    config.fileBackupDir shouldBe "dir"

    config.errorHandler shouldBe null
    config = config.withErrorHandler(new ErrorHandler {
      override def handle(e: _root_.java.lang.Throwable): Unit = ???
    })
    config.errorHandler shouldNotBe null

    val c = MessageCodecFactory.defaultFactoryForJSON.withCodecs(Map.empty)
    config = config.withMessageCodecFactory(c)
    config.codecFactory shouldBeTheSameInstanceAs c
  }

  test("create custom TDLogger") {
    val td = Fluentd.client
      .withTagPrefix("mydb")
      .newTDLogger("xxx/yyyyy", "api.treasuredata.co.jp")

    val td2 = td.withTagPrefix("another_db")
    td.close()
  }

  test("create debug console logger") {
    val f = Fluentd.client.newConsoleLoggerFactory()
    val l = f.getLogger
    l.emit("test_tag", Map("message" -> "hello"))
    val l2 = f.getTypedLogger[MyMetric]
    l2.emit(MyMetric(1, "leo"))
    f.close()
  }

  case class MyMetric(id: Int, name: String) extends TaggedMetric {
    override def metricTag: String = "airframe_metric_test"
  }

  test("send test") {
    skip("Production integration test")
    val f = Fluentd.client
      .withTagPrefix("leodb")
      .newTDLoggerFactory(apikey = sys.env("TD_API_KEY"))

    val td = f.getLogger
    td.emit("airframe_test", Map("c1" -> "hello", "c2" -> "fluentd", "c3" -> 10))
    td.emit("airframe_test", Map("c1" -> "hello", "c2" -> "airframe", "c3" -> 100))

    val l = f.getTypedLogger[MyMetric]
    l.emit(MyMetric(1, "leo"))
    l.emit(MyMetric(1, "yui"))

    f.close()
  }
}
