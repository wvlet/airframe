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
package wvlet.airframe.control

import java.io.File
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Resource that can be closed.
  * @tparam A
  */
trait Resource[A] extends AutoCloseable {
  def get: A
  def close(): Unit

  /**
    * Wrap a Future with this resource. After the future completes, the resource will be closed
    */
  def wrapFuture[U](body: A => Future[U])(implicit ec: ExecutionContext): Future[U] = {
    Future
      .apply(get)
      .flatMap { a: A => body(a) }
      .transform { case any =>
        Try(close())
        any
      }
  }
}

object Resource {

  /**
    * Create a resource for a temporary file, which will be deleted after closing the resource
    * @param name
    * @param suffix
    * @param dir
    * @return
    */
  def newTempFile(name: String, suffix: String = ".tmp", dir: String = "target"): Resource[File] = newResource[File](
    resource = {
      new File(dir)
      // Create the target directory if not exists
      val d = new File(dir)
      d.mkdirs()
      File.createTempFile(name, suffix, d)
    },
    onClose = { (file: File) =>
      file.delete
    }
  )

  /**
    * Create a new Resource from an AutoClosable object
    * @param resource
    * @tparam R
    * @return
    */
  def newResource[R](
      resource: R,
      onInit: R => Unit = { (x: R) => },
      onClose: R => Unit
  ): Resource[R] = {
    new Resource[R] {
      onInit(resource)
      override def get: R = resource
      override def close(): Unit = {
        onClose(resource)
      }
    }
  }
}
