/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import v2.connectors.ObligationsConnectorOutcome
import v2.models.domain.ObligationDetails
import v2.models.errors.{DownstreamError, Error}

object ObligationsHttpParser extends HttpParser {

  private val obligationsJsonReads: Reads[Seq[ObligationDetails]] = (__ \ "obligations").read[Seq[ObligationDetails]]
  private val multipleErrorJsonReads: Reads[Seq[Error]] = (__ \ "failures").read[Seq[Error]]

  implicit val  obligationsHttpReads: HttpReads[ObligationsConnectorOutcome] = new HttpReads[ObligationsConnectorOutcome] {
    override def read(method: String, url: String, response: HttpResponse): ObligationsConnectorOutcome = {

      val loggingPrefix = "[ObligationsHttpParser][obligationsHttpReads][read]"

      def parseErrors(response: HttpResponse): Seq[Error] = {
        val singleError = response.validateJson[Error].map(Seq(_))
        lazy val multipleErrors = response.validateJson[Seq[Error]](multipleErrorJsonReads)
        lazy val unableToParseJsonError = {
          Logger.warn(s"$loggingPrefix Unable to parse errors: ${response.body}")
          Seq(DownstreamError)
        }

        singleError orElse multipleErrors getOrElse unableToParseJsonError
      }

      response.status match {
        case OK => response.validateJson[Seq[ObligationDetails]](obligationsJsonReads) match {
          case Some(obligations) => Right(obligations)
          case None => Left(Seq(DownstreamError))
        }
        case _ =>
          val errors = parseErrors(response)
          Logger.warn(s"$loggingPrefix Get obligations returned the following error(s): ${errors.map(_.code).mkString(",")}")
          Left(errors)
      }
    }
  }
}
