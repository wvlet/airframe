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
package wvlet.airframe.arrow.flight.api

import wvlet.airframe.http.RPC
import wvlet.airframe.rx.RxStream

/*
 * A flight service is an endpoint for retrieving or storing Arrow data. A
 * flight service can expose one or more predefined endpoints that can be
 * accessed using the Arrow Flight Protocol. Additionally, a flight service
 * can expose a set of actions that are available.
 */
@RPC
trait FlightService {
  import FlightService._

  /*
   * Handshake between client and server. Depending on the server, the
   * handshake may be required to determine the token that should be used for
   * future operations. Both request and response are streams to allow multiple
   * round-trips depending on auth mechanism.
   */
  def handshake(handshakeRequest: RxStream[HandshakeRequest]): RxStream[HandshakeResponse]

  /*
   * Get a list of available streams given a particular criteria. Most flight
   * services will expose one or more streams that are readily available for
   * retrieval. This api allows listing the streams available for
   * consumption. A user can also provide a criteria. The criteria can limit
   * the subset of streams that can be listed via this interface. Each flight
   * service allows its own definition of how to consume criteria.
   */
  def listFlights(criteria: Criteria): RxStream[FlightInfo]

  /*
   * For a given FlightDescriptor, get information about how the flight can be
   * consumed. This is a useful interface if the consumer of the interface
   * already can identify the specific flight to consume. This interface can
   * also allow a consumer to generate a flight stream through a specified
   * descriptor. For example, a flight descriptor might be something that
   * includes a SQL statement or a Pickled Python operation that will be
   * executed. In those cases, the descriptor will not be previously availabale
   * within the list of available streams provided by ListFlights but will be
   * available for consumption for the duration defined by the specific flight
   * service.
   */
  def getFlightInfo(flightDescriptor: FlightDescriptor): FlightInfo

  /*
   * For a given FlightDescriptor, get the Schema as described in Schema.fbs::Schema
   * This is used when a consumer needs the Schema of flight stream. Similar to
   * GetFlightInfo this interface may generate a new flight that was not previously
   * available in ListFlights.
   */
  def getSchema(flightDescriptor: FlightDescriptor): SchemaResult

  /*
   * Retrieve a single stream associated with a particular descriptor
   * associated with the referenced ticket. A Flight can be composed of one or
   * more streams where each stream can be retrieved using a separate opaque
   * ticket that the flight service uses for managing a collection of streams.
   */
  def doGet(ticket: Ticket): RxStream[FlightData]

  /*
   * Push a stream to the flight service associated with a particular
   * flight stream. This allows a client of a flight service to upload a stream
   * of data. Depending on the particular flight service, a client consumer
   * could be allowed to upload a single stream per descriptor or an unlimited
   * number. In the latter, the service might implement a 'seal' action that
   * can be applied to a descriptor once all streams are uploaded.
   */
  def doPut(flightData: RxStream[FlightData]): RxStream[PutResult]

  /*
   * Open a bidirectional data channel for a given descriptor. This
   * allows clients to send and receive arbitrary Arrow data and
   * application-specific metadata in a single logical stream. In
   * contrast to DoGet/DoPut, this is more suited for clients
   * offloading computation (rather than storage) to a Flight service.
   */
  def doExchange(flightData: RxStream[FlightData]): RxStream[FlightData]

  /*
   * Flight services can support an arbitrary number of simple actions in
   * addition to the possible ListFlights, GetFlightInfo, DoGet, DoPut
   * operations that are potentially available. DoAction allows a flight client
   * to do a specific action against a flight service. An action includes
   * opaque request and response objects that are specific to the type action
   * being undertaken.
   */
  def doAction(action: Action): RxStream[Result]

  /*
   * A flight service exposes all of the available action types that it has
   * along with descriptions. This allows different flight consumers to
   * understand the capabilities of the flight service.
   */
  def listActions(): RxStream[ActionType]

}

object FlightService {
  /*
   * The request that a client provides to a server on handshake.
   */
  case class HandshakeRequest(
      /*
       * A defined protocol version
       */
      protocol_version: Long,
      /*
       * Arbitrary auth/handshake info.
       */
      payload: Array[Byte]
  )

  case class HandshakeResponse(
      /*
       * A defined protocol version
       */
      protocol_version: Long,
      /*
       * Arbitrary auth/handshake info.
       */
      payload: Array[Byte]
  )

  /*
   * A message for doing simple auth.
   */
  case class BasicAuth(
      username: String,
      password: String
  )

  /*
   * Describes an available action, including both the name used for execution
   * along with a short description of the purpose of the action.
   */
  case class ActionType(
      `type`: String,
      description: String
  ) {
    def actionType: String = `type`
  }
  /*
   * A service specific expression that can be used to return a limited set
   * of available Arrow Flight streams.
   */
  case class Criteria(
      expression: Array[Byte]
  )

  /*
   * An opaque action specific for the service.
   */
  case class Action(
      `type`: String,
      body: Array[Byte]
  ) {
    def actionType: String = `type`
  }

  /*
   * An opaque result returned after executing an action.
   */
  case class Result(
      body: Array[Byte]
  )

  /*
   * Wrap the result of a getSchema call
   */
  case class SchemaResult(
      // schema of the dataset as described in Schema.fbs::Schema.
      schema: Array[Byte]
  )

  /*
   * Describes what type of descriptor is defined.
   */
  sealed trait DescriptorType

  object DescriptorType {

    // Protobuf pattern, not used.
    case object UNKNOWN extends DescriptorType

    /*
     * A named path that identifies a dataset. A path is composed of a string
     * or list of strings describing a particular dataset. This is conceptually
     *  similar to a path inside a filesystem.
     */
    case object PATH extends DescriptorType

    /*
     * An opaque command to generate a dataset.
     */
    case object CMD extends DescriptorType

    def unapply(s: String): Option[DescriptorType] = {
      Seq(UNKNOWN, PATH, CMD).find(_.toString == s)
    }
  }

  /*
   * The name or tag for a Flight. May be used as a way to retrieve or generate
   * a flight or be used to expose a set of previously defined flights.
   */
  case class FlightDescriptor(
      `type`: DescriptorType,
      /*
       * Opaque value used to express a command. Should only be defined when
       * type = CMD.
       */
      cmd: Array[Byte],
      /*
       * List of strings identifying a particular dataset. Should only be defined
       * when type = PATH.
       */
      path: Seq[String]
  )

  /*
   * The access coordinates for retrieval of a dataset. With a FlightInfo, a
   * consumer is able to determine how to retrieve a dataset.
   */
  case class FlightInfo(
      // schema of the dataset as described in Schema.fbs::Schema.
      schema: Array[Byte],
      /*
       * The descriptor associated with this info.
       */
      flight_descriptor: FlightDescriptor,
      /*
       * A list of endpoints associated with the flight. To consume the whole
       * flight, all endpoints must be consumed.
       */
      endpoint: Seq[FlightEndpoint],
      // Set these to -1 if unknown.
      total_records: Long,
      total_bytes: Long
  )

  /*
   * A particular stream or split associated with a flight.
   */
  case class FlightEndpoint(
      /*
       * Token used to retrieve this stream.
       */
      ticket: Ticket,
      /*
       * A list of URIs where this ticket can be redeemed. If the list is
       * empty, the expectation is that the ticket can only be redeemed on the
       * current service where the ticket was generated.
       */
      location: Seq[Location]
  )

  /*
   * A location where a Flight service will accept retrieval of a particular
   * stream given a ticket.
   */
  case class Location(
      uri: String
  )

  /*
   * An opaque identifier that the service can use to retrieve a particular
   * portion of a stream.
   */
  case class Ticket(
      ticket: Array[Byte]
  )

  /*
   * A batch of Arrow data as part of a stream of batches.
   */
  case class FlightData(
      /*
       * The descriptor of the data. This is only relevant when a client is
       * starting a new DoPut stream.
       */
      flight_descriptor: FlightDescriptor,
      /*
       * Header for message data as described in Message.fbs::Message.
       */
      data_header: Array[Byte],
      /*
       * Application-defined metadata.
       */
      app_metadata: Array[Byte],
      /*
       * The actual batch of Arrow data. Preferably handled with minimal-copies
       * coming last in the definition to help with sidecar patterns (it is
       * expected that some implementations will fetch this field off the wire
       * with specialized code to avoid extra memory copies).
       */
      data_body: Array[Byte]
  )

  /**
    * The response message associated with the submission of a DoPut.
    */
  case class PutResult(
      app_metadata: Array[Byte]
  )
}
