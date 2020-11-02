/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import support.UnitSpec
import uk.gov.hmrc.http.HttpResponse
import v2.connectors.ObligationsConnectorOutcome
import v2.connectors.httpparsers.ObligationsHttpParser.obligationsHttpReads
import v2.models.domain.{FulfilledObligation, Obligation, ObligationDetails}
import v2.models.errors.{DownstreamError, Error}
import v2.models.outcomes.DesResponse

class ObligationsHttpParserSpec extends UnitSpec {

  val method: String = "GET"
  val url: String = "test-url"
  val correlationId = "x1234id"

  "read" should {

    "returns a collection of obligations" when {

      val validSuccessJson: JsValue = Json.parse(
        """
          |{
          |  "obligations": [
          |    {
          |      "identification": {
          |        "referenceType": "NINO",
          |        "referenceNumber": "AA123456A",
          |        "incomeSourceType": "ITSA"
          |      },
          |      "obligationDetails": [
          |        {
          |          "status": "F",
          |          "inboundCorrespondenceFromDate": "2018-01-01",
          |          "inboundCorrespondenceToDate": "2018-01-01",
          |          "inboundCorrespondenceDateReceived": "2018-01-01",
          |          "inboundCorrespondenceDueDate": "2018-01-01",
          |          "periodKey": ""
          |        }
          |      ]
          |    }
          |  ]
          |}""".stripMargin
      )

      val validSuccessData: Seq[ObligationDetails] = Seq(
        ObligationDetails(
          incomeSourceType = Some("ITSA"),
          obligations = Seq(
            Obligation(
              startDate = LocalDate.parse("2018-01-01"),
              endDate = LocalDate.parse("2018-01-01"),
              dueDate = LocalDate.parse("2018-01-01"),
              status = FulfilledObligation,
              processedDate = Some(LocalDate.parse("2018-01-01")),
              periodKey = ""
            )
          )
        )
      )

      "the HttpResponse has a 200 status and a correct response body" in {
        val response = HttpResponse(OK, validSuccessJson, Map("CorrelationId" -> Seq(correlationId)))
        val result: ObligationsConnectorOutcome = obligationsHttpReads.read(method, url, response)

        result shouldBe Right(DesResponse(correlationId,  validSuccessData))
      }
    }

    "returns a collection of one error" when {

      val invalidSuccessJson: JsValue = Json.parse(
        """
          |{
          |  "bad": "data"
          |}""".stripMargin
      )

      "the HttpResponse has a 200 status and an invalid response body" in {
        val response = HttpResponse(OK, invalidSuccessJson, Map("CorrelationId" -> Seq(correlationId)))
        val result: ObligationsConnectorOutcome = obligationsHttpReads.read(method, url, response)

        result shouldBe Left(DesResponse(correlationId, Seq(DownstreamError)))
      }
    }

    "returns a collection of multiple errors" when {

      val validMultipleErrorsJson: JsValue = Json.parse(
        """
          |{
          |  "failures": [
          |     {
          |         "code": "CODE_1",
          |         "reason": "error message"
          |     },
          |	    {
          |         "code": "CODE_2",
          |         "reason": "error message"
          |     }
          |  ]
          |}
          |""".stripMargin
      )

      val expectedErrors: Seq[Error] = Seq(
        Error("CODE_1", "error message"),
        Error("CODE_2", "error message")
      )

      "the HttpResponse has a 400 status and a multiple error response body" in {
        val response = HttpResponse(BAD_REQUEST, validMultipleErrorsJson, Map("CorrelationId" -> Seq(correlationId)))
        val result: ObligationsConnectorOutcome = obligationsHttpReads.read(method, url, response)

        result shouldBe Left(DesResponse(correlationId, expectedErrors))
      }
    }

    "returns a collection of one downstream error" when {

      val invalidErrorJson: JsValue = Json.parse(
        """
          |{
          |   "some": "error"
          |}
        """.stripMargin
      )

      "the HttpResponse has a 500 status" in {
        val response = HttpResponse(INTERNAL_SERVER_ERROR, invalidErrorJson, Map("CorrelationId" -> Seq(correlationId)))
        val result: ObligationsConnectorOutcome = obligationsHttpReads.read(method, url, response)

        result shouldBe Left(DesResponse(correlationId, Seq(DownstreamError)))
      }
    }

    val knownDesErrors = Map(
      "NOT_FOUND" -> NOT_FOUND,
      "NOT_FOUND_BPKEY" -> FORBIDDEN,
      "SERVICE_UNAVAILABLE" -> SERVICE_UNAVAILABLE,
      "SERVER_ERROR" -> INTERNAL_SERVER_ERROR,
      "INVALID_IDTYPE" -> BAD_REQUEST,
      "INVALID_IDNUMBER" -> BAD_REQUEST,
      "INVALID_STATUS" -> BAD_REQUEST,
      "INVALID_REGIME" -> BAD_REQUEST,
      "INVALID_DATE_TO" -> BAD_REQUEST,
      "INVALID_DATE_FROM" -> BAD_REQUEST,
      "INVALID_DATE_RANGE" -> BAD_REQUEST
    )

    knownDesErrors.foreach {
      case (code, status) => testKnownDesError(code, status)
    }

    def testKnownDesError(desErrorCode: String, status: Int): Unit = {
      val singleErrorJson: JsValue = Json.parse(
        s"""
           |{
           |   "code": "$desErrorCode",
           |   "reason": "some human readable message"
           |}
      """.stripMargin
      )

      "returns a collection of one error" when {
        val response = HttpResponse(NOT_FOUND, singleErrorJson, Map.empty[String,Seq[String]])
        lazy val result: ObligationsConnectorOutcome = obligationsHttpReads.read(method, url, response)

        s"the HttpResponse has a status code of $status and a error code of $desErrorCode" in {
          result match {
            case Left(DesResponse(_, error :: Nil)) => error.code shouldBe desErrorCode
            case Left(DesResponse(_,errors)) => fail(s"Expected 1 error but received ${errors.size}")
            case Right(_) => fail("Expected a left aligned result")
          }
        }
      }
    }

  }
}
