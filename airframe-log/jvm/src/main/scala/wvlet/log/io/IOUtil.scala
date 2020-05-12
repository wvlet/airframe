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
package wvlet.log.io

import java.io._
import java.net.{ServerSocket, URL}
import java.nio.charset.StandardCharsets

/**
  *
  */
object IOUtil {
  def withResource[Resource <: AutoCloseable, U](resource: Resource)(body: Resource => U): U = {
    try {
      body(resource)
    } finally {
      resource.close
    }
  }

  def withTempFile[U](name: String, suffix: String = ".tmp", dir: String = "target")(body: File => U) = {
    val d = new File(dir)
    d.mkdirs()
    val f = File.createTempFile(name, suffix, d)
    try {
      body(f)
    } finally {
      f.delete()
    }
  }

  def randomPort: Int = unusedPort
  def unusedPort: Int = {
    withResource(new ServerSocket(0)) { socket => socket.getLocalPort }
  }

  def findPath(path: String): Option[File] = findPath(new File(path))

  def findPath(path: File): Option[File] = {
    if (path.exists()) {
      Some(path)
    } else {
      val defaultPath = new File(new File(System.getProperty("prog.home", "")), path.getPath)
      if (defaultPath.exists()) {
        Some(defaultPath)
      } else {
        None
      }
    }
  }

  def readAsString(f: File): String = {
    readAsString(f.toURI.toURL)
  }

  def readAsString(url: URL): String = {
    withResource(url.openStream()) { in => readAsString(in) }
  }

  def readAsString(resourcePath: String): String = {
    require(resourcePath != null, s"resourcePath is null")
    Resource
      .find(resourcePath)
      .map(readAsString(_))
      .getOrElse {
        val file = findPath(new File(resourcePath))
        if (file.isEmpty) {
          throw new FileNotFoundException(s"Not found ${resourcePath}")
        }
        readAsString(new FileInputStream(file.get))
      }
  }

  def readAsString(in: InputStream): String = {
    readFully(in) { data => new String(data, StandardCharsets.UTF_8) }
  }

  def readFully[U](in: InputStream)(f: Array[Byte] => U): U = {
    val byteArray = withResource(new ByteArrayOutputStream) { b =>
      val buf = new Array[Byte](8192)
      withResource(in) { src =>
        var readBytes = 0
        while ({
          readBytes = src.read(buf);
          readBytes != -1
        }) {
          b.write(buf, 0, readBytes)
        }
      }
      b.toByteArray
    }
    f(byteArray)
  }
}
