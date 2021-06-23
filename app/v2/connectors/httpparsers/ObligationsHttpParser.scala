/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.connectors.httpparsers

import play.api.http.Status.OK
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import v2.connectors.ObligationsConnectorOutcome
import v2.models.domain.ObligationDetails
import v2.models.errors.{DownstreamError, Error}
import v2.models.outcomes.DesResponse
import v2.utils.Logging

object ObligationsHttpParser extends HttpParser with Logging {

  private val obligationsJsonReads: Reads[Seq[ObligationDetails]] = (__ \ "obligations").read[Seq[ObligationDetails]]
  private val multipleErrorJsonReads: Reads[Seq[Error]] = (__ \ "failures").read[Seq[Error]]

  implicit val obligationsHttpReads: HttpReads[ObligationsConnectorOutcome] = new HttpReads[ObligationsConnectorOutcome] {
    override def read(method: String, url: String, response: HttpResponse): ObligationsConnectorOutcome = {
      val correlationId = retrieveCorrelationId(response)

      val loggingPrefix = "[ObligationsHttpParser][obligationsHttpReads][read]"

      def parseErrors(response: HttpResponse): Seq[Error] = {
        val singleError = response.validateJson[Error].map(Seq(_))
        lazy val multipleErrors = response.validateJson[Seq[Error]](multipleErrorJsonReads)
        lazy val unableToParseJsonError = {
          logger.warn(s"$loggingPrefix Unable to parse errors: ${response.body}")
          Seq(DownstreamError)
        }

        singleError orElse multipleErrors getOrElse unableToParseJsonError
      }

      response.status match {
        case OK => response.validateJson[Seq[ObligationDetails]](obligationsJsonReads) match {
          case Some(obligations) => logger.info(s"$loggingPrefix - Successful response from DES with correlationId: $correlationId")
            Right(DesResponse(correlationId, obligations))
          case None => logger.warn(s"$loggingPrefix - downstream returned None. Revert to ISE for correlationId: $correlationId")
            Left(DesResponse(correlationId, Seq(DownstreamError)))
        }
        case _ =>
          val errors = parseErrors(response)
          logger.warn(s"$loggingPrefix Get obligations returned the following error(s): ${errors.map(_.code).mkString(",")} " +
            s"for correlationId: $correlationId")
          Left(DesResponse(correlationId, errors))
      }
    }
  }
}
