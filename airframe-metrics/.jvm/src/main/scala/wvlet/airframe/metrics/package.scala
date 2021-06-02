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

import java.time.{ZoneId, ZoneOffset, ZonedDateTime}

/**
  */
package object metrics {
  val systemTimeZone: ZoneOffset = {
    // Need to get the current ZoneOffset to resolve PDT, etc.
    // because ZoneID of America/Los Angels (PST) is -0800 while PDT zone offset is -0700
    val z = ZoneId.systemDefault().normalized() // This returns America/Los Angels (PST)
    ZonedDateTime.now(z).getOffset
  }

  val UTC: ZoneOffset = ZoneOffset.UTC
}
