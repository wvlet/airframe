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

import wvlet.airframe.rx._
import wvlet.log.LogSupport

import scala.concurrent.Promise
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

/**
  * Load the Google API https://github.com/google/google-api-javascript-client to use this component:
  *
  * {{{
  * <script src="https://apis.google.com/js/platform.js"></script>
  * }}}
  *
  * Usage:
  * {{{
  * val auth = new GoogleAuth(GoogleAuthConfig(clientId = "......"))
  * auth.getCurrentUser.transform {
  *   case Some(user) => ...
  *   case None => ...
  * }
  * }}}
  */
class GoogleAuth(config: GoogleAuthConfig) extends LogSupport {

  /**
    * The information of the signed-in user
    */
  private val currentUser: RxOptionVar[GoogleAuthProfile] = Rx.optionVariable(None)

  private val isInitialized = Promise[Boolean]()

  /**
    * Initialize GoogleAPI Auth2 and return the authenticated user profile if the user is already signed-in.
    */
  def getCurrentUser: RxOption[GoogleAuthProfile] = {
    if (!isInitialized.isCompleted) {
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
            if (isSignedIn) {
              updateUser
            } else {
              currentUser := None
            }
          })

          auth2.`then`({ () =>
            // Checking the sigh-in status here is necessary to properly wait
            // authentication completion at the initial attempt. Without this,
            // currentUser := None will be visible even though the user is already signed-in.
            val signedIn = isSignedIn
            debug(s"gapi.auth2 is initialized. signedIn: ${signedIn}")

            // Unblock the promise
            isInitialized.success(signedIn)
          })
        }
      )
      // Refresh auth token
      timers.setInterval(config.tokenRefreshIntervalMillis.toDouble) {
        refreshAuth
      }

      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
      Rx.fromFuture(isInitialized.future).transformOption {
        case Some(isSignedIn) => currentUser.get
        case None             => None
      }
    } else {
      // Just return the current user if gauth2 is already initialized
      currentUser
    }
  }

  private def getAuthInstance: js.Dynamic = {
    js.Dynamic.global.gapi.auth2.getAuthInstance()
  }

  def isSignedIn: Boolean = {
    getAuthInstance.isSignedIn.get().asInstanceOf[Boolean]
  }

  def signIn: Unit = {
    getAuthInstance.signIn()
  }

  def signOut: Unit = {
    getAuthInstance.signOut
    currentUser := None
    debug(s"Signed out")
  }

  def refreshAuth: Unit = {
    debug(s"Refreshing oauth2 token")
    val user = getAuthInstance.currentUser.get()
    user.reloadAuthResponse().`then` { () =>
      updateUser
    }
  }

  private def updateUser: Unit = {
    val googleUser = getAuthInstance.currentUser.get()
    val token      = googleUser.getAuthResponse().id_token
    val profile    = googleUser.getBasicProfile()
    currentUser := Some(
      GoogleAuthProfile(
        name = s"${profile.getName()}",
        email = s"${profile.getEmail()}",
        imageUrl = s"${profile.getImageUrl()}",
        id_token = token.toString
      )
    )
  }
}
