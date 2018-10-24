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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpResponse
import v2.mocks.connectors.MockDesConnector
import v2.mocks.services.MockAuditService
import v2.models.EopsDeclarationSubmission
import v2.models.audit.AuditEvent
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.outcomes.EopsDeclarationOutcome

import scala.concurrent.Future

class EopsDeclarationServiceSpec extends ServiceSpec {

  private trait Test extends MockAuditService with MockDesConnector {
    val nino: String = "AA123456A"
    val start: String = "2018-01-01"
    val end: String = "2018-12-31"

    val service = new EopsDeclarationService(mockAuditService, mockDesConnector)
  }

  val correlationId = "x1234id"

  "calling submit with valid arguments" should {

    "return None without errors " when {
      "successful request is made end des" in new Test {

        val httpResponse = HttpResponse(responseStatus = NO_CONTENT, None)

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Right(correlationId)))

        MockedAuditService.auditEventSucceeds(AuditEvent("auditType", "tx-name", "some details"))
        val result: Option[ErrorWrapper] = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))

        result shouldBe None
      }
    }

    "return multiple errors " when {
      "des connector returns sequence of errors" in new Test {

        val desResponse = MultipleErrors(Seq(MtdError("INVALID_ACCOUNTINGPERIODENDDATE", "some reason"),
          MtdError("INVALID_ACCOUNTINGPERIODSTARTDATE", "some reason")))

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Left(desResponse)))

        val expected = ErrorWrapper(BadRequestError, Some(Seq(InvalidEndDateError, InvalidStartDateError)))

        val result: Option[ErrorWrapper] = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))
        result shouldBe Some(expected)
      }
    }

    "return a single 500 (ISE) error" when {
      "multiple errors are returned that includes an INVALID_IDTYPE" in new Test {

        val desResponse = MultipleErrors(Seq(MtdError("INVALID_ACCOUNTINGPERIODENDDATE", "some reason"),
          MtdError("INVALID_IDTYPE", "'nino' type submitted is incorrect")))

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Left(desResponse)))

        val expected = ErrorWrapper(DownstreamError, None)

        val result: Option[ErrorWrapper] = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))

        result shouldBe Some(expected)
      }
    }

    "return multiple bvr errors " when {
      "des connector returns sequence of bvr errors" in new Test {

        val desResponse = BVRErrors(Seq(MtdError("C55317", "some reason"),
          MtdError("C55318", "some reason")))

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Left(desResponse)))

        val expected = ErrorWrapper(BVRError, Some(Seq(RuleClass4Over16, RuleClass4PensionAge)))

        val result: Option[ErrorWrapper] = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))

        result shouldBe Some(expected)
      }
    }

    "return single bvr error " when {
      "des connector returns single of bvr error" in new Test {

        val desResponse = BVRErrors(Seq(MtdError("C55317", "some reason")))

        MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
          .returns(Future.successful(Left(desResponse)))

        val expected = ErrorWrapper(RuleClass4Over16, None)

        val result: Option[ErrorWrapper] = await(service.submit(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(start), LocalDate.parse(end))))

        result shouldBe Some(expected)
      }
    }

    val possibleDesErrors: Seq[(String, String, MtdError)] = Seq(
      ("NOT_FOUND", "not found", NotFoundError),
      ("INVALID_IDTYPE", "downstream", DownstreamError),
      ("SERVICE_UNAVAILABLE", "service unavailable", ServiceUnavailableError),
      ("SERVER_ERROR", "downstream", DownstreamError),
      ("INVALID_IDVALUE", "invalid nino", NinoFormatError),
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

            val error: EopsDeclarationOutcome = Left(SingleError(MtdError(desCode, "")))

            MockedDesConnector.submitEOPSDeclaration(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))
              .returns(Future.successful(error))

            val result: Option[ErrorWrapper] = await(service.submit(EopsDeclarationSubmission(Nino(nino),
              LocalDate.parse(start), LocalDate.parse(end))))

            result shouldBe Some(ErrorWrapper(mtdError, None))
          }
        }
    }
  }
}
