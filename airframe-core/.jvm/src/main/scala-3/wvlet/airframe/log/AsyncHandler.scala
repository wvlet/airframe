package wvlet.airframe.log

import java.io.Flushable
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}
import java.util.logging as jl
import scala.util.control.NonFatal

/**
  * Logging using a background thread
  */
class AsyncHandler(parent: jl.Handler) extends jl.Handler with Guard with AutoCloseable with Flushable:
  private val executor =
    Executors.newSingleThreadExecutor(
      new ThreadFactory:
        override def newThread(r: Runnable): Thread =
          val t = new Thread(r, "AirframeLogAsyncHandler")
          t.setDaemon(true)
          t
    )

  private val queue      = new util.ArrayDeque[jl.LogRecord]
  private val isNotEmpty = newCondition
  private val closed     = new AtomicBoolean(false)

  // Start a poller thread
  executor.submit(
    new Runnable:
      override def run(): Unit =
        while !closed.get() do
          val record: jl.LogRecord = guard {
            if queue.isEmpty then isNotEmpty.await()
            queue.pollFirst()
          }
          if record != null then
            // Keep the poller thread alive if the parent handler fails, otherwise no further logs are written
            try parent.publish(record)
            catch
              case NonFatal(e) =>
                reportError(null, toException(e), jl.ErrorManager.WRITE_FAILURE)
  )

  private def toException(t: Throwable) = new Exception(t.getMessage, t)

  override def flush(): Unit =
    val records = Seq.newBuilder[jl.LogRecord]
    guard {
      while !queue.isEmpty do
        val record = queue.pollFirst()
        if record != null then records += record
    }

    records.result().map(parent.publish _)
    parent.flush()

  override def publish(record: jl.LogRecord): Unit =
    guard {
      queue.addLast(record)
      isNotEmpty.signal()
    }

  override def close(): Unit =
    flush()

    if closed.compareAndSet(false, true) then
      // Wake up the poller thread
      guard {
        isNotEmpty.signalAll()
      }
      executor.shutdownNow()

  def closeAndAwaitTermination(timeout: Int = 10, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Unit =
    close()
    while !executor.awaitTermination(timeout, timeUnit) do Thread.sleep(timeUnit.toMillis(timeout))
