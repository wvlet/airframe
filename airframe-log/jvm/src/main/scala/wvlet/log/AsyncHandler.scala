package wvlet.log

import java.io.Flushable
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.{logging => jl}

/**
  * Logging using background thread
  */
class AsyncHandler(parent: jl.Handler) extends jl.Handler with Guard with AutoCloseable with Flushable {
  private val executor = {
    Executors.newCachedThreadPool(
      new ThreadFactory {
        override def newThread(r: Runnable): Thread = {
          val t = new Thread(r, "WvletLogAsyncHandler")
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
        records += queue.pollFirst()
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
    if (closed.compareAndSet(false, true)) {
      flush()
      // Wake up the poller thread
      guard {
        isNotEmpty.signalAll()
      }
      executor.shutdown()
    }
  }
}
