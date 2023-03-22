package wvlet.log

import java.io.Flushable
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}
import java.util.{logging => jl}

/**
  * Logging using a background thread
  */
class AsyncHandler(parent: jl.Handler) extends jl.Handler with Guard with AutoCloseable with Flushable {
  private val executor = {
    Executors.newSingleThreadExecutor(
      new ThreadFactory {
        override def newThread(r: Runnable): Thread = {
          val t = new Thread(r, "AirframeLogAsyncHandler")
          t.setDaemon(true)
          t
        }
      }
    )
  }

  private val queue      = new util.ArrayDeque[jl.LogRecord]
  private val isNotEmpty = newCondition
  private val closed     = new AtomicBoolean(false)

  // Start a poller thread
  executor.submit(new Runnable {
    override def run(): Unit = {
      while (!closed.get()) {
        val record: jl.LogRecord = guard {
          if (queue.isEmpty) {
            isNotEmpty.await()
          }
          queue.pollFirst()
        }
        if (record != null) {
          parent.publish(record)
        }
      }
    }
  })

  override def flush(): Unit = {
    val records = Seq.newBuilder[jl.LogRecord]
    guard {
      while (!queue.isEmpty) {
        val record = queue.pollFirst()
        if (record != null) {
          records += record
        }
      }
    }

    records.result().map(parent.publish _)
    parent.flush()
  }

  override def publish(record: jl.LogRecord): Unit = {
    guard {
      queue.addLast(record)
      isNotEmpty.signal()
    }
  }

  override def close(): Unit = {
    flush()

    if (closed.compareAndSet(false, true)) {
      // Wake up the poller thread
      guard {
        isNotEmpty.signalAll()
      }
      executor.shutdownNow()
    }
  }

  def closeAndAwaitTermination(timeout: Int = 10, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Unit = {
    close()
    while (!executor.awaitTermination(timeout, timeUnit)) {
      Thread.sleep(timeUnit.toMillis(timeout))
    }
  }
}
