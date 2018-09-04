/*
 * Copyright 2018 HM Revenue & Customs
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

package v2.services

import java.time.LocalDate

import v2.mocks.connectors.MockDesConnector
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._

import scala.concurrent.Future

class EopsDeclarationServiceSpec extends ServiceSpec {

  private trait Test extends MockDesConnector {
    val service = new EopsDeclarationService(mockDesConnector)
  }

  "calling submit with invalid arguments" should {

    "return an invalid NINO error" when {
      "the NINO is in the wrong format" in new Test {
        val nino: String = "TEST"
        val start: String = "2018-01-01"
        val to: String = "2018-12-31"

        val expectedErrorResponse = ErrorResponse(InvalidNinoError, None)

        val result: Option[ErrorResponse] = await(service.submit(nino, start, to))
        result.get shouldBe expectedErrorResponse
      }
    }

    "return a missing start date error" when {
      "the start date is empty" in new Test {
        val nino: String = "AA123456A"
        val start: String = ""
        val end: String = "2018-12-31"

        val expectedErrorResponse = ErrorResponse(MissingStartDateError, None)

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))
        result.get shouldBe expectedErrorResponse
      }
    }

    "return an invalid start date error" when {

      val nino: String = "AA123456A"
      val end: String = "2018-12-31"

      "the start date is in the wrong format" in new Test {
        val start: String = "BOB"
        val expectedErrorResponse = ErrorResponse(InvalidStartDateError, None)

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))

        result.get shouldBe expectedErrorResponse
      }

      "the start date is an invalid date" in new Test {
        val start: String = "9999-99-99"
        val expectedErrorResponse = ErrorResponse(InvalidStartDateError, None)

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))

        result.get shouldBe expectedErrorResponse
      }
    }

    "return a missing end date error" when {
      "the end date is empty" in new Test {
        val nino: String = "AA123456A"
        val start: String = "2018-01-01"
        val end: String = ""
        val expectedErrorResponse = ErrorResponse(MissingEndDateError, None)

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))

        result.get shouldBe expectedErrorResponse
      }
    }

    "return invalid end date error" when {
      val nino: String = "AA123456A"
      val start: String = "2018-01-01"

      "the end date is in the wrong format" in new Test {
        val end: String = "BOB"
        val expectedErrorResponse = ErrorResponse(InvalidEndDateError, None)

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))

        result.get shouldBe expectedErrorResponse
      }

      "the start date is an invalid date" in new Test {
        val end: String = "9999-99-99"
        val expectedErrorResponse = ErrorResponse(InvalidEndDateError, None)

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))

        result.get shouldBe expectedErrorResponse
      }
    }

    "return multiple errors" when {
      "there are problems with more than one argument" in new Test {
        val nino: String = "AA123456A"
        val start: String = ""
        val end: String = ""

        val expectedErrorResponse = ErrorResponse(BadRequestError, Some(Seq(MissingStartDateError, MissingEndDateError)))

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))
        result.get shouldBe expectedErrorResponse
      }
    }
  }

  "calling submit with valid arguments" should {

    "return None without errors " when {
      "successful request is made end des" in new Test {
        val nino: String = "AA123456A"
        val start: String = "2018-01-01"
        val end: String = "2018-12-31"

        MockedDesConnector.submitEOPSDeclaration(nino, LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future{None})

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))
        result shouldBe None
      }
    }

    "return multiple errors " when {
      "des connector returns sequence of errors" in new Test {
        val nino: String = "AA123456A"
        val start: String = "2018-01-01"
        val end: String = "2018-12-31"

        val desResponse = MultipleErrors(Seq(Error("INVALID_ACCOUNTINGPERIODENDDATE", "some reason"),
          Error("INVALID_ACCOUNTINGPERIODSTARTDATE", "some reason")))

        MockedDesConnector.submitEOPSDeclaration(nino, LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future{Some(desResponse)})

        val expected = ErrorResponse(BadRequestError, Some(Seq(InvalidEndDateError,InvalidStartDateError)))

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))
        result.get shouldBe expected
      }
    }

    "return multiple bvr errors " when {
      "des connector returns sequence of bvr errors" in new Test {
        val nino: String = "AA123456A"
        val start: String = "2018-01-01"
        val end: String = "2018-12-31"

        val desResponse = MultipleBVRErrors(Seq(Error("C55317", "some reason"),
          Error("C55318", "some reason")))

        MockedDesConnector.submitEOPSDeclaration(nino, LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future{Some(desResponse)})

        val expected = ErrorResponse(BVRError, Some(Seq(RuleClass4Over16,RuleClass4PensionAge)))

        val result: Option[ErrorResponse] = await(service.submit(nino, start, end))
        result.get shouldBe expected
      }
    }

    val possibleDesErrors: Seq[(String, String, Error)] = Seq(
      ("NOT_FOUND", "not found", NotFoundError),
      ("INVALID_IDTYPE", "downstream", DownstreamError),
      ("SERVICE_UNAVAILABLE", "service unavailable", ServiceUnavailableError),
      ("SERVER_ERROR", "downstream", DownstreamError),
      ("INVALID_IDVALUE", "invalid nino", InvalidNinoError),
      ("INVALID_ACCOUNTINGPERIODENDDATE", "invalid end date", InvalidEndDateError),
      ("INVALID_ACCOUNTINGPERIODSTARTDATE", "invalid start date", InvalidStartDateError),
      ("CONFLICT", "duplicate submission", ConflictError),
      ("EARLY_SUBMISSION", "early submission", EarlySubmissionError),
      ("LATE_SUBMISSION", "late submission", LateSubmissionError)
    )

    possibleDesErrors.foreach {
      case (desCode, description, mtdError) =>
        s"return a $description error" when {
          s"the DES connector returns a $desCode code" in new Test {
            val nino: String = "AA123456A"
            val from: String = "2018-01-01"
            val to: String = "2018-12-31"

            val error: Future[Option[DesError]] = Future.successful(Some(SingleError(Error(desCode, ""))))

            MockedDesConnector.submitEOPSDeclaration(nino, LocalDate.parse(from), LocalDate.parse(to))
              .returns(error)

            val result: Option[ErrorResponse]  = await(service.submit(nino, from, to))
            result.get shouldBe ErrorResponse(mtdError, None)
          }
        }
    }


  }

}
