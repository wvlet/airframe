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
  * auth.init.transform {
  *   case None =>
  *     div("Loading ...")
  *   case _ =>
  *     auth.getCurrentUser.transform {
  *       case Some(userProfile) => ...
  *       case None => ...
  *     }
  * }
  * }}}
  */
class GoogleAuth(config: GoogleAuthConfig) extends LogSupport {

  /**
    * The information of the signed-in user
    */
  private val currentUser: RxOptionVar[GoogleAuthProfile] = Rx.optionVariable(None)

  private val initialSignInState = Promise[Boolean]()

  def getCurrentUserRx: RxOption[GoogleAuthProfile] = currentUser
  def getCurrentUser: Option[GoogleAuthProfile]     = currentUser.get

  /**
    * Initialize GoogleAPI Auth2 and return true if the user is already authenticated
    */
  def init: RxOption[Boolean] = {
    if (!initialSignInState.isCompleted) {
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
            if (!isSignedIn) {
              // If logged out somewhere, unset the current user
              currentUser := None
            }
          })

          auth2.`then`({ () =>
            // Checking the sigh-in status here is necessary to properly wait
            // authentication completion at the initial attempt. Without this,
            // currentUser := None will be visible even though the user is already signed-in.
            val signedIn = isSignedIn
            debug(s"gapi.auth2 is initialized. signedIn: ${signedIn}")
            if (signedIn) {
              updateUser
            }

            // Unblock the promise
            initialSignInState.success(signedIn)
          })
        }
      )
      // Refresh auth token
      timers.setInterval(config.tokenRefreshIntervalMillis.toDouble) {
        refreshAuth
      }
    }

    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    Rx.fromFuture(initialSignInState.future)
  }

  /**
    * Get the current Google Auth2 instance
    */
  def getAuthInstance: js.Dynamic = {
    js.Dynamic.global.gapi.auth2.getAuthInstance()
  }

  def isSignedIn: Boolean = {
    getAuthInstance.isSignedIn.get().asInstanceOf[Boolean]
  }

  /**
    * @param uxMode "popup" or "redirect"
    */
  def signIn(uxMode: String = "popup"): Unit = {
    val signInOptions = js.Dynamic.literal(
      ux_mode = uxMode
    )
    getAuthInstance
      .signIn(signInOptions).`then`({ () =>
        if (isSignedIn) {
          updateUser
        }
      })
  }

  def signOut: Unit = {
    getAuthInstance.signOut()
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
    currentUser := Some(readCurrentUser)
  }

  private def readCurrentUser: GoogleAuthProfile = {
    val googleUser = getAuthInstance.currentUser.get()
    val token      = googleUser.getAuthResponse().id_token
    val profile    = googleUser.getBasicProfile()
    GoogleAuthProfile(
      name = s"${profile.getName()}",
      email = s"${profile.getEmail()}",
      imageUrl = s"${profile.getImageUrl()}",
      id_token = token.toString
    )
  }
}
