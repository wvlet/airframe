package wvlet.log

import wvlet.log.LogFormatter.{BareFormatter, SourceCodeLogFormatter}
import wvlet.log.io.IOUtil._

/**
  *
  */
class AsyncHandlerTest extends Spec {

  "AsynHandler" should {

    "start background thread" in {
      val buf = new BufferedLogHandler(BareFormatter)
      withResource(new AsyncHandler(buf)) { h =>
        logger.addHandler(h)
        logger.setLogLevel(LogLevel.INFO)

        val l1 = "hello async logger"
        val l2 = "log output will be processed in a background thread"
        logger.info(l1)
        logger.warn(l2)
        logger.debug(l1) // should be ignored

        h.flush()

        val logs = buf.logs
        logs should have size 2
        logs(0) shouldBe l1
        logs(1) shouldBe l2
      }
    }

    "not block at the logging code" in {
      val buf = new BufferedLogHandler(BareFormatter)
      withResource(new AsyncHandler(buf)) { h =>
        logger.resetHandler(h)
        logger.setLogLevel(LogLevel.INFO)

        for(i <- (0 until 10000).par) {
          logger.info(s"hello world: ${i}")
        }
        val s = buf.logs.size
        s shouldBe < (10000)

        h.flush()
        buf.logs.size shouldBe 10000
      }
    }

  }
}
