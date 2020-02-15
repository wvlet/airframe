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
package wvlet.airframe

import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

import scala.concurrent.Future
import scala.language.higherKinds
import scala.util.Success

object TaglessFinalExample {
  trait Wrapper[F[_]] {
    def wrap[A](v: A): F[A]
  }

  class WebApp[F[_]](implicit w: Wrapper[F]) {
    def serverInfo: F[String] = w.wrap("hello")
  }

  implicit object FutureWrapper extends Wrapper[Future] {
    override def wrap[A](v: A): Future[A] = Future.successful(v)
  }
  implicit object OptionWrapper extends Wrapper[Option] {
    override def wrap[A](v: A): Option[A] = Option(v)
  }

  def webAppWithFuture = new WebApp[Future]
  def webAppWithOption = new WebApp[Option]
}

/**
  *
  */
class HigherKindedTypeBindingTest extends AirSpec {
  import TaglessFinalExample._

  def `support higher-kinded type binding`: Unit = {
    warn("TODO: This currently works only for JVM. Need to fix SurfaceMacros for Scala.js")
    val s = Surface.of[WebApp[Future]]
    debug(s)
    debug(s.typeArgs(0))

    val d = newDesign
      .bind[WebApp[Future]].toInstance(webAppWithFuture)
      .bind[WebApp[Option]].toInstance(webAppWithOption)
      .noLifeCycleLogging

    d.build[WebApp[Future]] { app => app.serverInfo.value shouldBe Some(Success("hello")) }

    d.build[WebApp[Option]] { app => app.serverInfo shouldBe Some("hello") }
  }
}
