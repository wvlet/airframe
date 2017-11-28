package wvlet.log

import wvlet.log.LogFormatter.BareFormatter
import wvlet.log.io.IOUtil._
import wvlet.log.io.Timer

/**
  *
  */
class AsyncHandlerTest extends Spec with Timer {

  "AsynHandler" should {
    "start background thread" in {
      val buf = new BufferedLogHandler(BareFormatter)
      withResource(new AsyncHandler(buf)) { h =>
        val logger = Logger("wvlet.log.asynctest")
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
      // We cannot use large N since Twitter's QueueingHandler drops the log requests upon high concurrent logging
      val N  = 100
      val R0 = 10
      val R1 = 1

      val al = Logger("wvlet.log.asynchronous")
      val sl = Logger("wvlet.log.synchronous")
      al.setLogLevel(LogLevel.INFO)
      sl.setLogLevel(LogLevel.INFO)

      val result = for ((handlerName, handler) <- Seq(("log-with-heavy-handler", HeavyHandler), ("log-with-fast-handler", NullHandler))) yield {
        time(s"${handlerName}", repeat = R0, blockRepeat = R1) {
          withResource(new AsyncHandler(handler)) { asyncHandler =>
            // async
            al.resetHandler(asyncHandler)
            // sync
            sl.resetHandler(handler)

            block("async") {
              for (i <- (0 until N).toIndexedSeq.par) {
                al.info(s"hello world: ${i}")
              }
            }

            block("sync") {
              for (i <- (0 until N).toIndexedSeq.par) {
                sl.info(s"hello world: ${i}")
              }
            }
          }
        }
      }
      val t = result(0) // heavy handler result
      t("async").averageWithoutMinMax should be < t("sync").averageWithoutMinMax
    }
  }
}

object HeavyHandler extends java.util.logging.Handler {
  override def flush(): Unit = {}
  override def publish(record: java.util.logging.LogRecord): Unit = {
    Thread.sleep(5)
  }
  override def close(): Unit = {}
}
