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
package wvlet.airframe.examples

import wvlet.airframe.AirframeSpec
import wvlet.airframe.examples.codec.Codec_01_SerDe
import wvlet.airframe.examples.di.{DI_01_HelloAirframe, DI_02_ConstructorInjection, DI_03_InTraitInjection}

/**
  *
  */
class ExamplesSpec extends AirframeSpec {

  "codec examples" taggedAs ("codec") in {
    Codec_01_SerDe.main(Array.empty)
  }

  "di examples" taggedAs ("di") in {
    DI_01_HelloAirframe.main(Array.empty)
    DI_02_ConstructorInjection.main(Array.empty)
    DI_03_InTraitInjection.main(Array.empty)
  }
}
