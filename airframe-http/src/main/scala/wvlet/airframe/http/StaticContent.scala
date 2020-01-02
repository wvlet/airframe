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
import wvlet.airframe.control.Control
import wvlet.log.LogSupport
import wvlet.log.io.{IOUtil, Resource}
import java.net.URL
import java.io.File

import scala.annotation.tailrec

/**
  * Helper for returning static contents
  */
object StaticContent extends LogSupport {

  trait ResourceType {
    def find(relativePath: String): Option[URL]
  }

  case class FileResource(basePath: String) extends ResourceType {
    override def find(relativePath: String): Option[URL] = {
      val f = new File(s"${basePath}/${relativePath}")
      if (f.exists()) {
        Some(f.toURI.toURL)
      } else {
        None
      }
    }
  }
  case class ClasspathResource(basePath: String) extends ResourceType {
    override def find(relativePath: String): Option[URL] = {
      Resource.find(s"${basePath}/${relativePath}")
    }
  }

  private def isSafeRelativePath(path: String): Boolean = {
    @tailrec
    def loop(pos: Int, path: List[String]): Boolean = {
      if (pos < 0) {
        false
      } else if (path.isEmpty) {
        true
      } else {
        if (path.head == "..") {
          loop(pos - 1, path.tail)
        } else {
          loop(pos + 1, path.tail)
        }
      }
    }

    loop(0, path.split("/").toList)
  }

  private def findContentType(filePath: String): String = {
    val leaf = filePath.split("/").lastOption.getOrElse("")
    val ext = {
      val pos = leaf.indexOf(".")
      if (pos > 0) {
        leaf.substring(pos + 1)
      } else {
        ""
      }
    }
    ext match {
      case "html" | "htm" => "text/html"
      case "gif"          => "image/gif"
      case "png"          => "image/png"
      case "jpeg" | "jpg" => "image/jpeg"
      case "css"          => "text/css"
      case "gz"           => "application/gzip"
      case "txt"          => "text/plain"
      case "xml"          => "application/xml"
      case "json"         => "application/json"
      case "zip"          => "application/zip"
      case "js"           => "application/javascript"
      case _              => "application/octet-stream"
    }
  }

  def fromResource(basePath: String, relativePath: String): SimpleHttpResponse = {
    StaticContent().fromResource(basePath).apply(relativePath)
  }

  def fromResource(basePaths: List[String], relativePath: String): SimpleHttpResponse = {
    val sc = basePaths.foldLeft(StaticContent()) { (sc, x) =>
      sc.fromResource(x)
    }
    sc.apply(relativePath)
  }

  def fromDirectory(dirPath: String, relativePath: String): SimpleHttpResponse = {
    StaticContent().fromDirectory(dirPath).apply(relativePath)
  }

  def fromDirectory(dirPaths: List[String], relativePath: String): SimpleHttpResponse = {
    val sc = dirPaths.foldLeft(StaticContent()) { (sc, x) =>
      sc.fromDirectory(x)
    }
    sc.apply(relativePath)
  }

  def fromResource(basePath: String): StaticContent  = StaticContent().fromResource(basePath)
  def fromDirectory(basePath: String): StaticContent = StaticContent().fromResource(basePath)
}

import StaticContent._

case class StaticContent(resourcePaths: List[ResourceType] = List.empty) {
  def fromDirectory(basePath: String): StaticContent = {
    this.copy(resourcePaths = FileResource(basePath) :: resourcePaths)
  }
  def fromResource(basePath: String): StaticContent = {
    this.copy(resourcePaths = ClasspathResource(basePath) :: resourcePaths)
  }

  def find(relativePath: String): Option[URL] = {
    @tailrec
    def loop(lst: List[ResourceType]): Option[URL] = {
      lst match {
        case Nil => None
        case resource :: tail =>
          resource.find(relativePath) match {
            case url @ Some(x) =>
              url
            case None =>
              loop(tail)
          }
      }
    }
    loop(resourcePaths)
  }

  def apply(relativePath: String): SimpleHttpResponse = {
    if (!isSafeRelativePath(relativePath)) {
      SimpleHttpResponse(HttpStatus.Forbidden_403)
    } else {
      find(relativePath)
        .map { uri =>
          val mediaType = findContentType(relativePath)
          // Read the resource file as binary
          Control.withResource(uri.openStream()) { in =>
            IOUtil.readFully(in) { content =>
              // TODO cache control (e.g., max-age, last-updated)
              SimpleHttpResponse(HttpStatus.Ok_200, content = content, contentType = Some(mediaType))
            }
          }
        }.getOrElse {
          SimpleHttpResponse(HttpStatus.NotFound_404)
        }
    }
  }
}
