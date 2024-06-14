package wvlet.log

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import wvlet.log.LogFormatter.{BareFormatter, ThreadLogFormatter}
import wvlet.log.io.IOUtil.*
import wvlet.log.io.Timer

/**
  */
class AsyncHandlerTest extends Spec with Timer {

  test("start background thread") {
    flaky {
      val buf = new BufferedLogHandler(ThreadLogFormatter)
      val l1  = "hello async logger"
      val l2  = "log output will be processed in a background thread"

      val handler = new AsyncHandler(buf)
      withResource(handler) { h =>
        val logger = Logger("internal.asynctest")
        logger.resetHandler(h)
        logger.setLogLevel(LogLevel.INFO)

        logger.info(l1)
        logger.warn(l2)
        logger.debug(l1) // should be ignored

        h.flush()
      }

      handler.closeAndAwaitTermination()
      val logs = buf.logs
      logs.size shouldBe 2
      logs(0).contains(l1) shouldBe true
      logs(1).contains(l2) shouldBe true
    }
  }

  test("not block at the logging code") {
    // We cannot use large N since Twitter's QueueingHandler drops the log requests upon high concurrent logging
    val N  = 10
    val R0 = 5
    val R1 = 1

    val al = Logger("wvlet.log.asynchronous")
    val sl = Logger("wvlet.log.synchronous")
    al.setLogLevel(LogLevel.INFO)
    sl.setLogLevel(LogLevel.INFO)

    sl.setLogLevel(LogLevel.INFO)

    def withThreadManager[U](body: ExecutorService => U): U = {
      val threadManager = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
      try {
        body(threadManager)
      } finally {
        threadManager.shutdown()
        while !threadManager.awaitTermination(10, TimeUnit.MILLISECONDS) do {}
      }
    }

    val result =
      for (
        (handlerName, handler) <- Seq(
          ("log-with-heavy-handler", HeavyHandler),
          ("log-with-fast-handler", NullHandler)
        )
      ) yield {
        time(s"${handlerName}", repeat = R0, blockRepeat = R1) {
          withResource(new AsyncHandler(handler)) { asyncHandler =>
            // async
            al.resetHandler(asyncHandler)
            // sync
            sl.resetHandler(handler)

            // Using a thread manager explicitly because of parallel collection issue of Scala 2.13.0-M4
            // import CompatParColls.Converters._
            block("async") {
              withThreadManager { threadManager =>
                for i <- (0 until N) do {
                  threadManager.submit(
                    new Runnable {
                      override def run(): Unit = {
                        al.info(s"hello world: ${i}")
                      }
                    }
                  )
                }
              }
            }

            block("sync") {
              withThreadManager { threadManager =>
                for i <- (0 until N) do {
                  threadManager.submit(
                    new Runnable {
                      override def run(): Unit = {
                        sl.info(s"hello world: ${i}")
                      }
                    }
                  )
                }
              }
            }
          }
        }
      }
    val t = result(0) // heavy handler result
    // t("async").averageWithoutMinMax should be < t("sync").averageWithoutMinMax
  }
}

object HeavyHandler extends java.util.logging.Handler {
  override def flush(): Unit = {}
  override def publish(record: java.util.logging.LogRecord): Unit = {
    Thread.sleep(5)
  }
  override def close(): Unit = {}
}
