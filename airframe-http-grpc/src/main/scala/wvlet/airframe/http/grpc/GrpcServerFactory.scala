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
package wvlet.airframe.http.grpc

import wvlet.airframe.Session
import wvlet.airframe.control.MultipleExceptions
import wvlet.log.LogSupport

import scala.collection.parallel.immutable.ParVector
import scala.util.control.NonFatal

/**
  * GrpcServerFactory manages
  *
  * @param session
  */
class GrpcServerFactory(session: Session) extends AutoCloseable with LogSupport {
  private var createdServers = List.empty[GrpcServer]

  def newServer(config: GrpcServerConfig): GrpcServer = {
    val server = config.newServer(session)
    synchronized {
      createdServers = server :: createdServers
    }
    server
  }

  def awaitTermination: Unit = {
    // Workaround for `.par` in Scala 2.13, which requires import scala.collection.parallel.CollectionConverters._
    // But this import doesn't work in Scala 2.12
    val b = ParVector.newBuilder[GrpcServer]
    b ++= createdServers
    b.result().foreach(_.awaitTermination)
  }

  override def close(): Unit = {
    debug(s"Closing GrpcServerFactory")
    val ex = Seq.newBuilder[Throwable]
    for (server <- createdServers) {
      try {
        server.close()
      } catch {
        case NonFatal(e) =>
          ex += e
      }
    }
    createdServers = List.empty

    val exceptions = ex.result()
    if (exceptions.nonEmpty) {
      if (exceptions.size == 1) {
        throw exceptions.head
      } else {
        throw MultipleExceptions(exceptions)
      }
    }
  }

}
