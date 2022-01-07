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

import wvlet.airspec.AirSpec

class GrpcStatusTest extends AirSpec {
  test("status code integer") {
    GrpcStatus.OK_0.code shouldBe 0
    GrpcStatus.CANCELLED_1.code shouldBe 1
    GrpcStatus.UNKNOWN_2.code shouldBe 2
    GrpcStatus.INVALID_ARGUMENT_3.code shouldBe 3
    GrpcStatus.DEADLINE_EXCEEDED_4.code shouldBe 4
    GrpcStatus.NOT_FOUND_5.code shouldBe 5
    GrpcStatus.ALREADY_EXISTS_6.code shouldBe 6
    GrpcStatus.PERMISSION_DENIED_7.code shouldBe 7
    GrpcStatus.RESOURCE_EXHAUSTED_8.code shouldBe 8
    GrpcStatus.FAILED_PRECONDITION_9.code shouldBe 9
    GrpcStatus.ABORTED_10.code shouldBe 10
    GrpcStatus.OUT_OF_RANGE_11.code shouldBe 11
    GrpcStatus.UNIMPLEMENTED_12.code shouldBe 12
    GrpcStatus.INTERNAL_13.code shouldBe 13
    GrpcStatus.UNAVAILABLE_14.code shouldBe 14
    GrpcStatus.DATA_LOSS_15.code shouldBe 15
    GrpcStatus.UNAUTHENTICATED_16.code shouldBe 16
  }

  test("code name") {
    GrpcStatus.OK_0.name shouldBe "OK"
    GrpcStatus.CANCELLED_1.name shouldBe "CANCELLED"
    GrpcStatus.UNKNOWN_2.name shouldBe "UNKNOWN"
    GrpcStatus.INVALID_ARGUMENT_3.name shouldBe "INVALID_ARGUMENT"
    GrpcStatus.DEADLINE_EXCEEDED_4.name shouldBe "DEADLINE_EXCEEDED"
    GrpcStatus.NOT_FOUND_5.name shouldBe "NOT_FOUND"
    GrpcStatus.ALREADY_EXISTS_6.name shouldBe "ALREADY_EXISTS"
    GrpcStatus.PERMISSION_DENIED_7.name shouldBe "PERMISSION_DENIED"
    GrpcStatus.RESOURCE_EXHAUSTED_8.name shouldBe "RESOURCE_EXHAUSTED"
    GrpcStatus.FAILED_PRECONDITION_9.name shouldBe "FAILED_PRECONDITION"
    GrpcStatus.ABORTED_10.name shouldBe "ABORTED"
    GrpcStatus.OUT_OF_RANGE_11.name shouldBe "OUT_OF_RANGE"
    GrpcStatus.UNIMPLEMENTED_12.name shouldBe "UNIMPLEMENTED"
    GrpcStatus.INTERNAL_13.name shouldBe "INTERNAL"
    GrpcStatus.UNAVAILABLE_14.name shouldBe "UNAVAILABLE"
    GrpcStatus.DATA_LOSS_15.name shouldBe "DATA_LOSS"
    GrpcStatus.UNAUTHENTICATED_16.name shouldBe "UNAUTHENTICATED"
  }

  test("http status mapping") {
    GrpcStatus.OK_0.httpStatus shouldBe HttpStatus.Ok_200
    GrpcStatus.CANCELLED_1.httpStatus shouldBe HttpStatus.ClientClosedRequest_499
    GrpcStatus.UNKNOWN_2.httpStatus shouldBe HttpStatus.InternalServerError_500
    GrpcStatus.INVALID_ARGUMENT_3.httpStatus shouldBe HttpStatus.BadRequest_400
    GrpcStatus.DEADLINE_EXCEEDED_4.httpStatus shouldBe HttpStatus.GatewayTimeout_504
    GrpcStatus.NOT_FOUND_5.httpStatus shouldBe HttpStatus.NotFound_404
    GrpcStatus.ALREADY_EXISTS_6.httpStatus shouldBe HttpStatus.Conflict_409
    GrpcStatus.PERMISSION_DENIED_7.httpStatus shouldBe HttpStatus.Forbidden_403
    GrpcStatus.RESOURCE_EXHAUSTED_8.httpStatus shouldBe HttpStatus.TooManyRequests_429
    GrpcStatus.FAILED_PRECONDITION_9.httpStatus shouldBe HttpStatus.BadRequest_400
    GrpcStatus.ABORTED_10.httpStatus shouldBe HttpStatus.Conflict_409
    GrpcStatus.OUT_OF_RANGE_11.httpStatus shouldBe HttpStatus.BadRequest_400
    GrpcStatus.UNIMPLEMENTED_12.httpStatus shouldBe HttpStatus.NotImplemented_501
    GrpcStatus.INTERNAL_13.httpStatus shouldBe HttpStatus.InternalServerError_500
    GrpcStatus.UNAVAILABLE_14.httpStatus shouldBe HttpStatus.ServiceUnavailable_503
    GrpcStatus.DATA_LOSS_15.httpStatus shouldBe HttpStatus.InternalServerError_500
    GrpcStatus.UNAUTHENTICATED_16.httpStatus shouldBe HttpStatus.Unauthorized_401
  }
}
