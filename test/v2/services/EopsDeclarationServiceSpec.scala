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

package v2.services

import java.time.LocalDate

import uk.gov.hmrc.domain.Nino
import v2.connectors.EopsDeclarationConnectorOutcome
import v2.mocks.connectors.MockDesConnector
import v2.models.auth.UserDetails
import v2.models.domain.EopsDeclarationSubmission
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.outcomes.DesResponse

import scala.concurrent.Future

class EopsDeclarationServiceSpec extends ServiceSpec {

  private trait Test extends MockDesConnector {
    implicit val userDetails: UserDetails = UserDetails("123456890", "Individual", None)

    val nino: String = "AA123456A"
    val start: String = "2018-01-01"
    val end: String = "2018-12-31"

    val service = new EopsDeclarationService(mockDesConnector)
  }

  "calling submit with valid arguments" should {

    "return None without errors " when {
      "successful request is made end des" in new Test {

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Right(DesResponse(correlationId, ()))))

        val result: EopsDeclarationOutcome = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))

        result shouldBe Right(DesResponse(correlationId, ()))
      }
    }

    "return multiple errors " when {
      "des connector returns sequence of errors" in new Test {

        val desResponse: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(Error("INVALID_ACCOUNTINGPERIODENDDATE", "some reason"),
          Error("INVALID_ACCOUNTINGPERIODSTARTDATE", "some reason"))))

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Left(desResponse)))

        val expected: ErrorWrapper = ErrorWrapper(correlationId, BadRequestError, Some(Seq(InvalidEndDateError, InvalidStartDateError)))

        val result: EopsDeclarationOutcome = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))
        result shouldBe Left(expected)
      }
    }

    "return a single 500 (ISE) error" when {
      "multiple errors are returned that includes an INVALID_IDTYPE" in new Test {

        val desResponse: DesResponse[MultipleErrors] = DesResponse(correlationId, MultipleErrors(Seq(Error("INVALID_ACCOUNTINGPERIODENDDATE", "some reason"),
          Error("INVALID_IDTYPE", "'nino' type submitted is incorrect"))))

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Left(desResponse)))

        val expected: ErrorWrapper = ErrorWrapper(correlationId, DownstreamError, None)

        val result: EopsDeclarationOutcome = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))

        result shouldBe Left(expected)
      }
    }

    "return multiple bvr errors " when {
      "des connector returns sequence of bvr errors" in new Test {

        val desResponse: DesResponse[BVRErrors] = DesResponse(correlationId, BVRErrors(Seq(Error("C55317", "some reason"),
          Error("C55318", "some reason"))))

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Left(desResponse)))

        val expected: ErrorWrapper = ErrorWrapper(correlationId, BVRError, Some(Seq(RuleClass4Over16, RuleClass4PensionAge)))

        val result: EopsDeclarationOutcome = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))

        result shouldBe Left(expected)
      }
    }

    "return single bvr error " when {
      "des connector returns single of bvr error" in new Test {

        val desResponse: DesResponse[BVRErrors] = DesResponse(correlationId, BVRErrors(Seq(Error("C55317", "some reason"))))

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Left(desResponse)))

        val expected: ErrorWrapper = ErrorWrapper(correlationId, RuleClass4Over16, None)

        val result: EopsDeclarationOutcome = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))

        result shouldBe Left(expected)
      }
    }

    val possibleDesErrors: Seq[(String, String, Error)] = Seq(
      ("NOT_FOUND", "not found", NotFoundError),
      ("INVALID_IDTYPE", "downstream", DownstreamError),
      ("SERVICE_UNAVAILABLE", "service unavailable", ServiceUnavailableError),
      ("SERVER_ERROR", "downstream", DownstreamError),
      ("INVALID_IDVALUE", "invalid nino", NinoFormatError),
      ("INVALID_ACCOUNTINGPERIODENDDATE", "invalid end date", InvalidEndDateError),
      ("INVALID_ACCOUNTINGPERIODSTARTDATE", "invalid start date", InvalidStartDateError),
      ("CONFLICT", "duplicate submission", ConflictError),
      ("EARLY_SUBMISSION", "early submission", EarlySubmissionError),
      ("LATE_SUBMISSION", "late submission", LateSubmissionError),
      ("NON_MATCHING_PERIOD", "non matching period", NonMatchingPeriodError)
    )

    possibleDesErrors.foreach {
      case (desCode, description, mtdError) =>
        s"return a $description error" when {
          s"the DES connector returns a $desCode code" in new Test {

            val error: EopsDeclarationConnectorOutcome = Left(DesResponse(correlationId, SingleError(Error(desCode, ""))))

            MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
              .returns(Future.successful(error))

            val result: EopsDeclarationOutcome = await(service.submit(EopsDeclarationSubmission(Nino(nino),
              LocalDate.parse(start), LocalDate.parse(end))))

            result shouldBe Left(ErrorWrapper(correlationId, mtdError, None))
          }
        }
    }
  }
}
