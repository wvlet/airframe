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
import wvlet.log.LogSupport
import wvlet.log.io.{IOUtil, Resource}

import scala.annotation.tailrec

/**
 * Helper for returning static contents
 */
object StaticContent extends LogSupport {

  private def isSafeRelativePath(path:String): Boolean = {
    @tailrec
    def loop(pos:Int, path:List[String]): Boolean = {
      if(pos < 0) {
        false
      }
      else if(path.isEmpty) {
        true
      }
      else {
        if(path.head == "..") {
          loop(-1, path.tail)
        }
        else {
          loop(pos+1, path.tail)
        }
      }
    }

    loop(0, path.split("/").toList)
  }

  def fromResource(basePath:String, relativePath:String): SimpleHttpResponse = {
    val resourcePath = s"${basePath}/${relativePath}"
    if(!isSafeRelativePath(relativePath)) {
      SimpleHttpResponse(HttpStatus.Forbidden_403)
    }
    else {
      Resource.find(resourcePath).map { uri =>
        val content = IOUtil.readAsString(uri)
        // TODO resolve the content type from file suffix
        // TODO: Read as binary
        SimpleHttpResponse(HttpStatus.Ok_200, content)
      }.getOrElse {
        SimpleHttpResponse(HttpStatus.NotFound_404)
      }
    }
  }
}
