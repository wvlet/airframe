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
package wvlet.airframe.http

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.internal.RPCCallContext
import wvlet.airframe.surface.{MethodSurface, Parameter, Surface, TypeName}
import wvlet.airframe.ulid.ULID
import wvlet.log.{AsyncHandler, LogFormatter, LogRecord, LogRotationHandler, LogTimestampFormatter}

import java.lang.reflect.InvocationTargetException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import scala.annotation.tailrec
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionException
import scala.util.Try

case class HttpAccessLogConfig(
    fileName: String = "log/http_access.json",
    maxFiles: Int = 100,
    maxSize: Long = 100 * 1024 * 1024
)

/**
  */
trait HttpAccessLogWriter extends AutoCloseable {
  def write(log: Map[String, Any]): Unit
}

object HttpAccessLogWriter {

  def default = new JSONHttpAccessLogWriter()

  /**
    * Creates an in-memory log writer. This is only for testing purpose. Do not use it in production.
    */
  def inMemoryLogWriter = new InMemoryAccessLogWriter()

  /**
    * Write access logs to a file using a JSON format. This writer supports automatic log file rotation.
    *
    * @param httpAccessLogConfig
    */
  class JSONHttpAccessLogWriter(httpAccessLogConfig: HttpAccessLogConfig = HttpAccessLogConfig())
      extends HttpAccessLogWriter {

    private val mapCodec = MessageCodec.of[Map[String, Any]]

    object JSONLogFormatter extends LogFormatter {
      override def formatLog(r: LogRecord): String = {
        val m = r.getMessage
        m
      }
    }

    // Use an async handler to perform logging in a background thread
    private val asyncLogHandler = new AsyncHandler(
      new LogRotationHandler(
        fileName = httpAccessLogConfig.fileName,
        maxNumberOfFiles = httpAccessLogConfig.maxFiles,
        maxSizeInBytes = httpAccessLogConfig.maxSize,
        formatter = JSONLogFormatter,
        logFileExt = ".json"
      )
    )

    override def write(log: Map[String, Any]): Unit = {
      // Generate one-liner JSON log
      // TODO: Handle too large log data (e.g., binary data)
      val json = mapCodec.toJson(log)
      asyncLogHandler.publish(new java.util.logging.LogRecord(Level.INFO, json))
    }

    override def close(): Unit = {
      asyncLogHandler.close()
    }
  }

  /**
    * In-memory log writer for testing purpose. Not for production use.
    */
  class InMemoryAccessLogWriter extends HttpAccessLogWriter {
    private var logs = Seq.newBuilder[Map[String, Any]]

    def getLogs: Seq[Map[String, Any]] = logs.result()

    def clear(): Unit = {
      logs.clear()
    }

    override def write(log: Map[String, Any]): Unit = {
      synchronized {
        logs += log
      }
    }

    override def close(): Unit = {
      // no-op
    }
  }

  private[http] def errorLog(e: Throwable): ListMap[String, Any] = {

    /**
      * Find the root cause of the exception from wrapped exception classes
      */
    @tailrec
    def findCause(e: Throwable): Throwable = {
      e match {
        case i: InvocationTargetException if i.getTargetException != null =>
          findCause(i.getTargetException)
        case ee: ExecutionException if ee.getCause != null =>
          findCause(ee.getCause)
        case _ =>
          e
      }
    }

    val m = ListMap.newBuilder[String, Any]
    // Resolve the cause of the exception
    findCause(e) match {
      case null =>
      // no-op
      case se: HttpServerException =>
        // If the cause is provided, record it. Otherwise, recording the status_code is sufficient.
        if (se.getCause != null) {
          val rootCause = findCause(se.getCause)
          m += "exception"         -> rootCause
          m += "exception_message" -> rootCause.getMessage
        }
      case other =>
        m += "exception"         -> other
        m += "exception_message" -> other.getMessage
    }
    m.result()
  }

  private[http] def rpcLog(rpcContext: RPCCallContext): ListMap[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "rpc_interface" -> TypeName.sanitizeTypeName(rpcContext.rpcInterfaceCls.getName)
    m += "rpc_class"     -> rpcContext.rpcMethodSurface.owner.fullName
    m += "rpc_method"    -> rpcContext.rpcMethodSurface.name

    val rpcArgs = extractRpcArgLog(rpcContext)
    if (rpcArgs.nonEmpty) {
      m += "rpc_args" -> rpcArgs
    }
    m.result()
  }

  private[http] def extractRpcArgLog(rpcContext: RPCCallContext): ListMap[String, Any] = {

    def traverseObject(s: Surface, arg: Any): ListMap[String, Any] = {
      val builder = ListMap.newBuilder[String, Any]
      s.params
        .foreach { p =>
          Try(builder ++= traverseParam(p, p.get(arg)))
        }
      builder.result()
    }

    def traverseParam(p: Parameter, arg: Any): ListMap[String, Any] = {
      arg match {
        case r: HttpMessage.Request =>
          ListMap.empty
        case r if p.surface.fullName == "com.twitter.finagle.http.Request" =>
          ListMap.empty
        case c: HttpContext[_, _, _] =>
          ListMap.empty
        case _ if p.isSecret =>
          ListMap.empty
        case u: ULID =>
          // Fixes https://github.com/wvlet/airframe/issues/1715
          ListMap(p.name -> u)
        case _ if p.surface.params.length > 0 =>
          ListMap(p.name -> traverseObject(p.surface, arg))
        case _ =>
          ListMap(p.name -> arg)
      }
    }

    val rpcArgsBuilder = ListMap.newBuilder[String, Any]
    // Exclude request context objects, which will be duplicates of request parameter logs
    for ((p, arg) <- rpcContext.rpcMethodSurface.args.zip(rpcContext.rpcArgs)) {
      rpcArgsBuilder ++= traverseParam(p, arg)
    }
    rpcArgsBuilder.result()
  }

  import scala.jdk.CollectionConverters._
  private val headerSanitizeCache = new ConcurrentHashMap[String, String]().asScala

  private[http] def sanitizeHeader(h: String): String = {
    headerSanitizeCache.getOrElseUpdate(h, h.replaceAll("-", "_").toLowerCase(Locale.ENGLISH))
  }

  private[http] def sanitize(s: String): String = {
    s.map {
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c    => c
    }.mkString
  }

  def logUnixTime: ListMap[String, Any] = {
    val currentTimeMillis = System.currentTimeMillis()
    // Unix time
    ListMap(
      "time" -> (currentTimeMillis / 1000L),
      // timestamp with ms resolution and zone offset
      "event_time" -> LogTimestampFormatter.formatTimestampWithNoSpaace(currentTimeMillis)
    )
  }

}
