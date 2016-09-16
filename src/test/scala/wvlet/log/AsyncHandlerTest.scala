package wvlet.log

//import com.twitter.{logging => tw}
import wvlet.log.LogFormatter.BareFormatter
import wvlet.log.io.IOUtil._

/**
  *
  */
class AsyncHandlerTest extends Spec {

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
      val N = 100
      val R0 = 100
      val R1 = 1

      val al = Logger("wvlet.log.asynchronous")
      val sl = Logger("wvlet.log.synchronous")
      al.setLogLevel(LogLevel.INFO)
      sl.setLogLevel(LogLevel.INFO)

//      val tal = tw.Logger("wvlet.log.twasync")
//      val tsl = tw.Logger("wvlet.log.twsync")
//      tal.setLevel(tw.Level.INFO)
//      tsl.setLevel(tw.Level.INFO)

      val t = time("logger", repeat = R0, blockRepeat = R1) {
        val b1 = HeavyHandler
        val b2 = HeavyHandler

        withResource(new AsyncHandler(b1)) { h =>
          // async
          al.resetHandler(h)
          // sync
          sl.resetHandler(b2)

//          // com.twitter.logging async
//          tal.clearHandlers()
//          tal.addHandler(tw.QueueingHandler(handler = () => TwitterHeavyHandler)())
//          tal.setUseParentHandlers(false)
//
//          // com.twitte.logging sync
//          tsl.clearHandlers()
//          tsl.setUseParentHandlers(false)
//          tsl.addHandler(TwitterHeavyHandler)

          block("async") {
            for (i <- (0 until N).par) {
              al.info(s"hello world: ${i}")
            }
          }

          block("sync") {
            for (i <- (0 until N).par) {
              sl.info(s"hello world: ${i}")
            }
          }

          // com.twitter.util does not support Scala 2.12
//          block("twitter-async") {
//            for (i <- (0 until N).par) {
//              tal.info(s"hello world: ${i}")
//            }
//          }
//
//          block("twitter-sync") {
//            for (i <- (0 until N).par) {
//              tsl.info(s"hello world: ${i}")
//            }
//          }
        }
      }
      t("async") should be < t("sync")
    }
  }
}

object HeavyHandler extends java.util.logging.Handler {
  override def flush(): Unit = {}
  override def publish(record: java.util.logging.LogRecord): Unit = {
    Thread.sleep(1)
  }
  override def close(): Unit = {}
}

//object TwitterHeavyHandler extends tw.Handler(tw.BareFormatter, None) {
//  def publish(record: java.util.logging.LogRecord) {
//    Thread.sleep(1)
//  }
//  def close() {}
//  def flush() {}
//
//  // for java compatibility
//  def get(): this.type = this
//}
