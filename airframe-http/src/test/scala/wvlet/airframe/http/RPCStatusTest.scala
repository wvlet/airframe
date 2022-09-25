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

import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.msgpack.spi.{MessagePack, Value}
import wvlet.airspec.AirSpec

class RPCStatusTest extends AirSpec {

  test("Have no duplicates and no gaps") {
    var knownCodes = Set.empty[Int]

    var counter                            = 0
    var currentType: Option[RPCStatusType] = None

    RPCStatus.all.foreach { x =>
      // Bump the counter for each statusType
      if (currentType.isEmpty || currentType != Some(x.statusType)) {
        currentType = Some(x.statusType)
        counter = x.statusType.minCode
      }
      knownCodes.contains(x.code) shouldBe false
      knownCodes += x.code
      x.code shouldBe counter
      counter += 1

      // sanity test
      val errorDetails = s"${x}[${x.code}] ${x.grpcStatus} ${x.httpStatus}"
      debug(errorDetails)
    }
  }

  test("ofCode(code) maps to the right code") {
    RPCStatus.all.foreach { x =>
      RPCStatus.ofCode(x.code) shouldBe x
    }
  }

  test("ofCode('unknown code')") {
    intercept[IllegalArgumentException] {
      RPCStatus.ofCode(-1) shouldBe None
    }
  }

  test("ofCodeName maps to the right code") {
    RPCStatus.all.foreach { x =>
      RPCStatus.ofCodeName(x.name) shouldBe x
    }
  }

  test("ofCodeName('unknown code name')") {
    intercept[IllegalArgumentException] {
      RPCStatus.ofCodeName("INVALID_CODE_000") shouldBe None
    }
  }

  test("serialize as integer") {
    RPCStatus.all.foreach { x =>
      val packer = MessagePack.newBufferPacker
      x.pack(packer)
      val msgpack = packer.toByteArray

      val v = ValueCodec.fromMsgPack(msgpack)
      v shouldBe Value.LongValue(x.code)

    // TODO Support unapply(v: Value) is supported in airframe-codec
    // val codec = MessageCodec.of[RPCStatus]
    // val s1    = codec.fromMsgPack(msgpack)
    // info(s1)
    }
  }

  test("fromGrpcStatusCode") {
    GrpcStatus.all.foreach { x =>
      val status = RPCStatus.fromGrpcStatusCode(x.code)
      status.grpcStatus shouldBe x
    }
  }

  test("Generate Markdown Table") {
    val list = RPCStatus.all.map { x =>
      s"| ${x.name} | ${x.statusType} | ${x.grpcStatus} | ${x.httpStatus.code}: ${x.httpStatus.reason} |"
    }
    val header =
      s"""| RPCStatus | Type | gRPC Status | Http Status |
       >|------------|------|-------------|-------------|""".stripMargin('>')

    debug(s"${header}\n${list.mkString("\n")}")
  }

  test("fromHttpStatus") {
//    // The code for generating mappings
//    val statusMappings = HttpStatus.knownStatuses
//      .sortBy(_.code)
//      .map { s =>
//        s"${s.getClass.getSimpleName.stripSuffix("$")} -> ${RPCStatus.fromGrpcStatusCode(GrpcStatus.ofHttpStatus(s).code)}"
//      }.mkString(",\n")
//
    for (code <- 0 to 600) {
      val h = HttpStatus.ofCode(code)
      val r = RPCStatus.fromHttpStatus(HttpStatus.ofCode(code))
      h.isSuccessful shouldBe r.isSuccess
    }
  }
}
