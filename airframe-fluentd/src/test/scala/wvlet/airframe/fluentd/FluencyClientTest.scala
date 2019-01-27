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
import org.komamitsu.fluency.fluentd.FluencyBuilderForFluentd
import org.komamitsu.fluency.fluentd.ingester.FluentdIngester
import org.komamitsu.fluency.ingester.sender.ErrorHandler
import wvlet.airframe.AirframeSpec

/**
  *
  */
class FluencyClientTest extends AirframeSpec {

  "should configure Fluency by default" in {
    val fluentdConfig  = FluentdConfig()
    val fluencyConfig  = FluencyConfig()
    val fluency        = FluencyClient.newFluency(fluentdConfig, fluencyConfig)
    val defaultFluency = new FluencyBuilderForFluentd().build()
    try {
      // Fluency.toString returns string representation of Buffer and Flusher settings
      val config        = fluency.toString.split("@").head
      val defaultConfig = defaultFluency.toString.split("@").head
      config shouldBe defaultConfig

      // Check senderMaxRetryCount and sslEnabled
      val senderConfig        = fluency.getFlusher.getIngester.getSender.toString.split("@").head
      val defaultSenderConfig = defaultFluency.getFlusher.getIngester.getSender.toString.split("@").head
      senderConfig shouldBe defaultSenderConfig

    } finally {
      fluency.close()
      defaultFluency.close()
    }
  }

  "should configure Fluency by specified configuration" in {
    val fluentdConfig = FluentdConfig()
    val fluencyConfig = FluencyConfig(
      useExtendedEventTime = true,
      maxBufferSize = 1024 * 1024 * 10,
      bufferChunkInitialSize = 1024 * 1024 * 5,
      bufferChunkRetentionSize = 1024 * 1024 * 10,
      bufferChunkRetentionTimeMillis = 5000,
      flushIntervalMillis = 30000,
      fileBackupDir = "./target/FluentcyClientTestTemp",
      waitUntilBufferFlushed = 5,
      waitUntilFlusherTerminated = 5,
      jvmHeapBufferMode = true,
      errorHandler = new ErrorHandler { override def handle(e: Throwable): Unit = ??? },
      senderMaxRetryCount = 8,
      ackResponseMode = true,
      sslEnabled = true
    )
    val fluency = FluencyClient.newFluency(fluentdConfig, fluencyConfig)
    try {
      fluency.getBuffer.getMaxBufferSize shouldBe 1024 * 1024 * 10
      fluency.getBuffer.getChunkInitialSize shouldBe 1024 * 1024 * 5
      fluency.getBuffer.getChunkRetentionSize shouldBe 1024 * 1024 * 10
      fluency.getBuffer.getChunkRetentionTimeMillis shouldBe 5000
      fluency.getFlusher.getFlushIntervalMillis shouldBe 30000
      fluency.getBuffer.getFileBackupDir shouldBe "./target/FluentcyClientTestTemp"
      fluency.getFlusher.getWaitUntilBufferFlushed shouldBe 5
      fluency.getFlusher.getWaitUntilTerminated shouldBe 5
      fluency.getBuffer.getJvmHeapBufferMode shouldBe true
      fluency.getFlusher.getIngester.asInstanceOf[FluentdIngester].isAckResponseMode shouldBe true
      val senderInfo = fluency.getFlusher.getIngester.getSender.toString
      senderInfo.indexOf("maxRetryCount=8") shouldBe >(0)
      senderInfo.indexOf("baseSender=SSLSender") shouldBe >(0)

    } finally {
      fluency.close()
    }
  }
}
