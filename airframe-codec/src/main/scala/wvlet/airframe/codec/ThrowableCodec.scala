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
package wvlet.airframe.codec

import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.surface.Surface

/**
  * Codec for Exception (Throwable) classes
  */
object ThrowableCodec extends MessageCodec[Throwable] {

  private lazy val genericExceptionSurface = Surface.of[GenericException]
  // This needs to be lazy as MessageCodecFactory will load ThrowableCodec
  private lazy val genericExceptionCodec =
    MessageCodecFactory.defaultFactoryForJSON
      .ofSurface(genericExceptionSurface).asInstanceOf[MessageCodec[GenericException]]

  override def pack(p: Packer, v: Throwable): Unit = {
    val m = GenericException.fromThrowable(v)
    genericExceptionCodec.pack(p, m)
  }
  // We do not support deserialization of generic Throwable classes
  override def unpack(u: Unpacker, v: MessageContext): Unit = {
    genericExceptionCodec.unpack(u, v)
  }
}

/**
  * Generic representation of Throwable for RPC messaging and logging exception
  * @param exceptionClass
  * @param message
  * @param stackTrace
  * @param cause
  */
case class GenericException(
    exceptionClass: String,
    message: String,
    stackTrace: Seq[GenericStackTraceElement] = Seq.empty,
    cause: Option[GenericException] = None
) extends Throwable(message, cause.getOrElse(null)) {

  // Populate the stack trace when the parent Throwable constructor is called
  override def fillInStackTrace(): Throwable = {
    setStackTrace(getStackTrace)
    this
  }
  override def getStackTrace: Array[StackTraceElement] = stackTrace.map(_.toJavaStackTraceElement).toArray
}

object GenericException {
  def fromThrowable(e: Throwable, seen: Set[Throwable] = Set.empty): GenericException = {
    val exceptionClass = e.getClass.getName
    val message        = Option(e.getMessage).getOrElse(e.getClass.getSimpleName)

    val stackTrace = for (x <- e.getStackTrace) yield {
      GenericStackTraceElement(
        className = x.getClassName,
        methodName = x.getMethodName,
        fileName = Option(x.getFileName),
        lineNumber = x.getLineNumber
      )
    }

    val cause = Option(e.getCause).flatMap { ce =>
      if (seen.contains(ce)) {
        None
      } else {
        Some(GenericException.fromThrowable(ce, seen + e))
      }
    }

    GenericException(
      exceptionClass = exceptionClass,
      message = message,
      // materialize stack trace just in case
      stackTrace = stackTrace.toIndexedSeq,
      cause = cause
    )
  }
}

/**
  * Generic stacktrace representation
  */
case class GenericStackTraceElement(
    className: String,
    methodName: String,
    fileName: Option[String],
    lineNumber: Int
) {
  override def toString: String = {
    val fileLoc = fileName.map(x => s"(${x}:${lineNumber})").getOrElse("")
    s"${className}:${methodName}${fileLoc}"
  }
  def toJavaStackTraceElement: StackTraceElement = {

    new StackTraceElement(className, methodName, fileName.getOrElse(null), lineNumber)
  }
}
