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
package wvlet.airframe.http.client

import wvlet.airframe.http.HttpMessage.Message

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, SequenceInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.{Collections, UUID}


object MultipartMessage {
  private val CRLF = "\r\n".getBytes(StandardCharsets.UTF_8)
  private val Dashes = "--".getBytes(StandardCharsets.UTF_8)
}

case class MultipartMessage(parts: Seq[FormDataPart]) extends Message {
  import MultipartMessage.*

  override def toContentString: String = ???
  override def toContentBytes: Array[Byte] = ???

  lazy val boundary: String = s"------AirframeHttpMultipartBoundary${UUID.randomUUID().toString}"
  private val boundaryBytes = boundary.getBytes(StandardCharsets.UTF_8)

  def toInputStream: InputStream = {
    if(parts.isEmpty) {
      val finalBoundary = new ByteArrayOutputStream()
      finalBoundary.write(Dashes)
      finalBoundary.write(boundaryBytes)
      finalBoundary.write(Dashes)
      finalBoundary.write(CRLF)
      new ByteArrayInputStream(finalBoundary.toByteArray)
    }
    else {
      // Create an enumeration of InputStreams for each piece: boundary, headers, content, CRLF
      val streams = parts.flatMap { part =>
        getInputStreamForPart(part)
      } :+ getInputStreamForFinalBoundary() // Add final boundary stream

      // SequenceInputStream concatenates them virtually
      new SequenceInputStream(Collections.enumeration(scala.jdk.CollectionConverters.SeqHasAsJava(streams).asJava))
    }
  }

  // Helper to write a full part to a ByteArrayOutputStream (used by contentBytes)
  private def writePart(out: ByteArrayOutputStream, part: FormDataPart): Unit = {
    // Boundary line
    out.write(Dashes)
    out.write(boundaryBytes)
    out.write(CRLF)
    // Headers
    part match {
      case StringPart(name, value, ctOpt) =>
        out.write(s"""Content-Disposition: form-data; name="$name"""".getBytes(StandardCharsets.UTF_8))
        out.write(CRLF)
        ctOpt.foreach(ct => out.write(s"Content-Type: $ct\r\n".getBytes(StandardCharsets.UTF_8)))
        out.write(CRLF) // End headers
        out.write(value.getBytes(StandardCharsets.UTF_8)) // Content
      case FilePart(name, filename, data, contentType) =>
        out.write(s"""Content-Disposition: form-data; name="$name"; filename="$filename"""".getBytes(StandardCharsets.UTF_8))
        out.write(CRLF)
        out.write(s"Content-Type: $contentType".getBytes(StandardCharsets.UTF_8))
        out.write(CRLF)
        out.write(CRLF) // End headers
        // Read data (WARNING: In-memory read for this helper)
        out.write(data) // Content
    }
    out.write(CRLF) // Final CRLF for the part
  }


  // Helper for toInputStream: Creates sequence of streams for a single part
  private def getInputStreamForPart(part: FormDataPart): Seq[InputStream] = {
    val headerStream = new ByteArrayOutputStream()
    // Boundary line
    headerStream.write(Dashes)
    headerStream.write(boundaryBytes)
    headerStream.write(CRLF)
    // Headers
    part match {
      case StringPart(name, value, ctOpt) =>
        headerStream.write(s"""Content-Disposition: form-data; name="$name"""".getBytes(StandardCharsets.UTF_8))
        headerStream.write(CRLF)
        ctOpt.foreach(ct => headerStream.write(s"Content-Type: $ct\r\n".getBytes(StandardCharsets.UTF_8)))
        headerStream.write(CRLF) // End headers
        Seq(
          new ByteArrayInputStream(headerStream.toByteArray),
          new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)), // Content Stream
          new ByteArrayInputStream(CRLF) // Part CRLF Stream
        )
      case FilePart(name, filename, dataSource, contentType) =>
        headerStream.write(s"""Content-Disposition: form-data; name="$name"; filename="$filename"""".getBytes(StandardCharsets.UTF_8))
        headerStream.write(CRLF)
        headerStream.write(s"Content-Type: $contentType".getBytes(StandardCharsets.UTF_8))
        headerStream.write(CRLF)
        headerStream.write(CRLF) // End headers

        val contentStream: InputStream = dataSource match {
          case Left(path) => Files.newInputStream(path) // True streaming from file
          case Right(isProvider) => isProvider() // Use the provided stream supplier
        }
        Seq(
          new ByteArrayInputStream(headerStream.toByteArray),
          contentStream, // Content Stream
          new ByteArrayInputStream(CRLF) // Part CRLF Stream
        )
    }
  }

  // Helper for toInputStream: Creates stream for the final boundary
  private def getInputStreamForFinalBoundary(): InputStream = {
    val finalBoundary = new ByteArrayOutputStream()
    finalBoundary.write(Dashes)
    finalBoundary.write(boundaryBytes)
    finalBoundary.write(Dashes)
    finalBoundary.write(CRLF)
    new ByteArrayInputStream(finalBoundary.toByteArray)
  }
}


sealed trait FormDataPart {
  def name: String
  def writeTo(out: ByteArrayOutputStream, boundary: String): Unit

  protected val CRLF = "\r\n".getBytes(StandardCharsets.UTF_8)

  protected def writeHeader(out: ByteArrayOutputStream, boundary: String, headers: Seq[String]): Unit = {
    out.write(s"--${boundary}\r\n".getBytes(StandardCharsets.UTF_8))
    headers.foreach { case h =>
      out.write(s"${h}\r\n".getBytes(StandardCharsets.UTF_8))
    }
    out.write(CRLF) // End of headers
  }
}

case class StringPart(name: String, value: String, contentType: Option[String] = None) extends FormDataPart {
  override def writeTo(out: ByteArrayOutputStream, boundary: String): Unit = {
    val disposition = s"""Content-Disposition: form-data; name="${name}""""
    val ctHeader    = contentType.map(ct => s"Content-Type: $ct").toSeq
    val headers     = Seq(disposition) ++ ctHeader

    writeHeader(out, boundary, headers)
    out.write(value.getBytes(StandardCharsets.UTF_8))
    out.write(CRLF)
  }
}

case class FilePart(
    name: String,
    filename: String,
    data: Array[Byte],  // Could also use InputStream or Path for streaming/large files
    contentType: String // e.g., "image/jpeg", "application/octet-stream"
) extends FormDataPart {
  override def writeTo(out: ByteArrayOutputStream, boundary: String): Unit = {
    val disposition = s"""Content-Disposition: form-data; name="$name"; filename="$filename""""
    val ctHeader    = s"Content-Type: $contentType"
    val headers     = Seq(disposition, ctHeader)

    writeHeader(out, boundary, headers)
    out.write(data)
    out.write(CRLF)
  }
}

object FilePart {
  def fromPath(name: String, path: Path, contentType: Option[String] = None): FilePart = {
    val resolvedContentType = contentType.getOrElse {
      try { Option(Files.probeContentType(path)).getOrElse("application/octet-stream") }
      catch { case _: Exception => "application/octet-stream" }
    }
    FilePart(
      name = name,
      filename = path.getFileName.toString,
      data = Files.readAllBytes(path), // WARNING: Loads entire file into memory!
      contentType = resolvedContentType
    )
  }

  // Add variants for InputStream if needed, but handle resource closing carefully!
}
