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
package wvlet.airframe.rx.html.widget.auth

/**
  */
import org.scalajs.dom
import wvlet.airframe.rx.{Rx, RxOptionVar, RxVar}
import wvlet.airframe.rx.html.all._
import wvlet.log.LogSupport

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.timers

/**
  */
case class GoogleAuthProfile(
    name: String,
    email: String,
    imageUrl: String,
    id_token: String
)

case class GoogleAuthConfig(
    clientId: String,
    // Refresh OAuth token every 45 minutes
    tokenRefreshIntervalMillis: Long = 45 * 60 * 1000
)

object GoogleAuth extends LogSupport {
  val currentUser: RxOptionVar[GoogleAuthProfile] = Rx.optionVariable(None)
  private[auth] val loading                       = Rx.variable(true)

  /**
    * Initialize GoogleAPI Auth2, and return a Future, which will be set to true
    * after the initialization completed.
    */
  def init(config: GoogleAuthConfig): Future[Boolean] = {
    val isInitialized = Promise[Boolean]()
    js.Dynamic.global.gapi.load(
      "auth2",
      () => {
        val auth2 = js.Dynamic.global.gapi.auth2
          .init(
            js.Dynamic
              .literal(
                client_id = config.clientId,
                fetch_basic_profile = true
              )
          )

        auth2.isSignedIn.listen((isSignedIn: Boolean) => {
          debug(s"isSignedIn: ${isSignedIn}")
          if (isSignedIn) {
            updateUser
          } else {
            GoogleAuth.currentUser := None
          }
        })

        auth2.`then`({ () =>
          debug(s"gapi.auth2 is initialized")
          // Show the login button
          loading := false
          isInitialized.success(true)
        })
      }
    )

    // Refresh auth token
    timers.setInterval(config.tokenRefreshIntervalMillis) {
      refreshAuth
    }

    isInitialized.future
  }

  private[auth] def signOut: Unit = {
    val auth2 = js.Dynamic.global.gapi.auth2.getAuthInstance()
    auth2.signOut()
    GoogleAuth.currentUser := None
    debug(s"Signed out")
    dom.document.location.reload()
  }

  private def refreshAuth: Unit = {
    debug(s"Refreshing oauth2 token")
    val user = js.Dynamic.global.gapi.auth2.getAuthInstance().currentUser.get()
    user.reloadAuthResponse().`then` { () =>
      updateUser
    }
  }

  private def updateUser: Unit = {
    val googleUser = js.Dynamic.global.gapi.auth2.getAuthInstance().currentUser.get()
    val token      = googleUser.getAuthResponse().id_token
    val profile    = googleUser.getBasicProfile()
    GoogleAuth.currentUser := Some(
      GoogleAuthProfile(
        name = s"${profile.getName()}",
        email = s"${profile.getEmail()}",
        imageUrl = s"${profile.getImageUrl()}",
        id_token = token.toString
      )
    )
  }
}
