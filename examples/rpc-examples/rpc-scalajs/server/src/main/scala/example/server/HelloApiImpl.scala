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
package example.server

import example.api.HelloApi
import example.api.HelloApi.TableData

/**
  *
  */
class HelloApiImpl extends HelloApi {
  override def hello(message: String): String = {
    s"Hello ${message}!!"
  }

  override def getTable: HelloApi.TableData = {
    TableData(
      columnNames = Seq("id", "name"),
      rows = Seq(
        Seq("1", "leo"),
        Seq("2", "aina"),
        Seq("3", "yui"),
      )
    )
  }
}
