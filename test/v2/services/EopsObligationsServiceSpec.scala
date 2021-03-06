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

import v2.connectors.ObligationsConnectorOutcome
import v2.mocks.connectors.MockDesConnector
import v2.models.domain.{FulfilledObligation, Obligation, ObligationDetails}
import v2.models.errors.GetEopsObligationsErrors._
import v2.models.errors._
import v2.models.outcomes.DesResponse

import scala.concurrent.Future

class EopsObligationsServiceSpec extends ServiceSpec {

  private trait Test extends MockDesConnector {
    val service = new EopsObligationsService(mockDesConnector)
  }

  val validNonEopsObligationsData: Seq[ObligationDetails] = Seq(
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
    ),
    ObligationDetails(
      incomeSourceType = Some("ITSP"),
      obligations = Seq(
        Obligation(
          startDate = LocalDate.parse("2018-02-01"),
          endDate = LocalDate.parse("2018-02-01"),
          dueDate = LocalDate.parse("2018-02-01"),
          status = FulfilledObligation,
          processedDate = Some(LocalDate.parse("2018-02-01")),
          periodKey = ""
        )
      )
    ),
    ObligationDetails(
      incomeSourceType = None,
      obligations = Seq(
        Obligation(
          startDate = LocalDate.parse("2018-02-01"),
          endDate = LocalDate.parse("2018-02-01"),
          dueDate = LocalDate.parse("2018-02-01"),
          status = FulfilledObligation,
          processedDate = Some(LocalDate.parse("2018-02-01")),
          periodKey = "EOPS"
        ),
        Obligation(
          startDate = LocalDate.parse("2018-02-01"),
          endDate = LocalDate.parse("2018-02-01"),
          dueDate = LocalDate.parse("2018-02-01"),
          status = FulfilledObligation,
          processedDate = Some(LocalDate.parse("2018-02-01")),
          periodKey = ""
        )
      )
    )
  )

  val validObligationsData: Seq[ObligationDetails] = Seq(
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
    ),
    ObligationDetails(
      incomeSourceType = Some("ITSP"),
      obligations = Seq(
        Obligation(
          startDate = LocalDate.parse("2018-02-01"),
          endDate = LocalDate.parse("2018-02-01"),
          dueDate = LocalDate.parse("2018-02-01"),
          status = FulfilledObligation,
          processedDate = Some(LocalDate.parse("2018-02-01")),
          periodKey = ""
        )
      )
    ),
    ObligationDetails(
      incomeSourceType = Some("ITSP"),
      obligations = Seq(
        Obligation(
          startDate = LocalDate.parse("2018-02-01"),
          endDate = LocalDate.parse("2018-02-01"),
          dueDate = LocalDate.parse("2018-02-01"),
          status = FulfilledObligation,
          processedDate = Some(LocalDate.parse("2018-02-01")),
          periodKey = "EOPS"
        ),
        Obligation(
          startDate = LocalDate.parse("2018-02-01"),
          endDate = LocalDate.parse("2018-02-01"),
          dueDate = LocalDate.parse("2018-02-01"),
          status = FulfilledObligation,
          processedDate = Some(LocalDate.parse("2018-02-01")),
          periodKey = ""
        )
      )
    )
  )

  val successEopsObligations: Seq[Obligation] = Seq(
    Obligation(
      startDate = LocalDate.parse("2018-02-01"),
      endDate = LocalDate.parse("2018-02-01"),
      dueDate = LocalDate.parse("2018-02-01"),
      status = FulfilledObligation,
      processedDate = Some(LocalDate.parse("2018-02-01")),
      periodKey = "EOPS"
    )
  )

  val emptyObligationsData: Seq[ObligationDetails] = Seq(
    ObligationDetails(
      incomeSourceType = Some("ITSB"),
      obligations = Seq(
        Obligation(
          startDate = LocalDate.parse("2018-02-01"),
          endDate = LocalDate.parse("2018-02-01"),
          dueDate = LocalDate.parse("2018-02-01"),
          status = FulfilledObligation,
          processedDate = Some(LocalDate.parse("2018-02-01")),
          periodKey = "EOPS"
        )
      )
    ),
    ObligationDetails(
      incomeSourceType = Some("ITSP"),
      obligations = Seq(
        Obligation(
          startDate = LocalDate.parse("2018-02-01"),
          endDate = LocalDate.parse("2018-02-01"),
          dueDate = LocalDate.parse("2018-02-01"),
          status = FulfilledObligation,
          processedDate = Some(LocalDate.parse("2018-02-01")),
          periodKey = "EOP"
        )
      )
    )
  )

  val emptyEopsObligations: Seq[Obligation] = Seq.empty

  "calling filterEopsObligations" should {

    "return only ITSP EOPS obligations details" when {
      "various obligation types are returned" in new Test {
        service.filterEopsObligations(validObligationsData) shouldBe successEopsObligations
      }
    }

    "return empty obligations details" when {
      "no ISTP EOPS obligation types are returned" in new Test {
        service.filterEopsObligations(emptyObligationsData) shouldBe emptyEopsObligations
      }
    }

  }

  "calling retrieveEopsObligations with invalid arguments" should {

    "return an invalid NINO error" when {
      "the NINO is in the wrong format" in new Test {
        val nino: String = "BOB"
        val from: String = "2018-01-01"
        val to: String = "2018-12-31"

        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, NinoFormatError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(expectedErrorResponse)
      }
    }

    "return a missing from date error" when {
      "the from date is empty" in new Test {
        val nino: String = "AA123456A"
        val from: String = ""
        val to: String = "2018-12-31"

        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, MissingFromDateError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(expectedErrorResponse)
      }
    }

    "return an invalid from date error" when {

      val nino: String = "AA123456A"
      val to: String = "2018-12-31"

      "the from date is in the wrong format" in new Test {
        val from: String = "BOB"
        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, InvalidFromDateError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))

        result shouldBe Left(expectedErrorResponse)
      }

      "the from date is an invalid date" in new Test {
        val from: String = "9999-99-99"
        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, InvalidFromDateError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))

        result shouldBe Left(expectedErrorResponse)
      }
    }

    "return a missing to date error" when {
      "the to date is empty" in new Test {
        val nino: String = "AA123456A"
        val from: String = "2018-01-01"
        val to: String = ""
        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, MissingToDateError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))

        result shouldBe Left(expectedErrorResponse)
      }
    }

    "return invalid to date error" when {
      val nino: String = "AA123456A"
      val from: String = "2018-01-01"

      "the to date is in the wrong format" in new Test {
        val to: String = "BOB"
        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, InvalidToDateError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))

        result shouldBe Left(expectedErrorResponse)
      }

      "the from date is an invalid date" in new Test {
        val to: String = "9999-99-99"
        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, InvalidToDateError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))

        result shouldBe Left(expectedErrorResponse)
      }
    }

    "return an invalid range error" when {
      "the to date is before the from date" in new Test {
        val nino: String = "AA123456A"
        val from: String = "2018-12-31"
        val to: String = "2018-01-01"

        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, RangeToDateBeforeFromDateError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(expectedErrorResponse)
      }

      "the from date is the same as the to date" in new Test {
        val nino: String = "AA123456A"
        val from: String = "2018-01-01"
        val to: String = "2018-01-01"

        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, RangeToDateBeforeFromDateError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(expectedErrorResponse)
      }
    }

    "return a range too big error" when {
      "the days between the from date and to date range is 367 days" in new Test {
        val nino: String = "AA123456A"
        val from: String = "2018-01-01"
        val to: String = "2019-01-03" // 367 days difference

        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, RangeTooBigError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(expectedErrorResponse)
      }

      "the days between the from date and to date range is 368 days" in new Test {
        val nino: String = "AA123456A"
        val from: String = "2018-01-01"
        val to: String = "2019-01-04" // 368 days difference

        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, RangeTooBigError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(expectedErrorResponse)
      }
    }

    "return multiple errors" when {
      "the there are problems with more than one argument" in new Test {
        val nino: String = "AA123456A"
        val from: String = ""
        val to: String = ""

        val expectedErrorResponse: ErrorWrapper = ErrorWrapper(correlationId, BadRequestError, Some(Seq(MissingFromDateError, MissingToDateError)))

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(expectedErrorResponse)
      }
    }
  }

  "calling retrieveEopsObligations with valid arguments" should {

    "return multiple obligations details" when {
      "successful request is made to des" in new Test {
        val nino: String = "AA123456A"
        val from: String = "2018-01-01"
        val to: String = "2018-12-31"

        val outcomesObligations: Future[ObligationsConnectorOutcome] = Future(Right(DesResponse(correlationId, validObligationsData)))

        MockedDesConnector.getObligations(nino, LocalDate.parse(from), LocalDate.parse(to))
          .returns(outcomesObligations)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Right(DesResponse(correlationId, successEopsObligations))
      }
    }

    "return Not Found error" when {
      "no ITSP EOPS obligations types are returned" in new Test {
        val nino: String = "AA123456A"
        val from: String = "2018-01-01"
        val to: String = "2018-12-31"

        val outcomesObligations: Future[ObligationsConnectorOutcome] = Future(Right(DesResponse(correlationId, validNonEopsObligationsData)))

        MockedDesConnector.getObligations(nino, LocalDate.parse(from), LocalDate.parse(to))
          .returns(outcomesObligations)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(ErrorWrapper(correlationId, NotFoundError, None))
      }
    }

    val possibleDesErrors: Seq[(String, String, Error)] = Seq(
      (DownstreamError.code, "downstream", DownstreamError),
      ("NOT_FOUND", "not found", NotFoundError),
      ("NOT_FOUND_BPKEY", "downstream", DownstreamError),
      ("SERVICE_UNAVAILABLE", "service unavailable", DownstreamError),
      ("SERVER_ERROR", "downstream", DownstreamError),
      ("INVALID_IDTYPE", "not found", NotFoundError),
      ("INVALID_IDNUMBER", "invalid nino", NinoFormatError),
      ("INVALID_STATUS", "downstream", DownstreamError),
      ("INVALID_REGIME", "downstream", DownstreamError),
      ("INVALID_DATE_TO", "invalid to date", InvalidToDateError),
      ("INVALID_DATE_FROM", "invalid from date", InvalidFromDateError),
      ("INVALID_DATE_RANGE", "invalid date range", RangeTooBigError)
    )

    possibleDesErrors.foreach {
      case (desCode, description, mtdError) =>
        s"return a $description error" when {
          s"the DES connector returns a $desCode code" in new Test {
            val nino: String = "AA123456A"
            val from: String = "2018-01-01"
            val to: String = "2018-12-31"

            val error: Future[ObligationsConnectorOutcome] = Future.successful(Left(DesResponse(correlationId, Seq(Error(desCode, "")))))

            MockedDesConnector.getObligations(nino, LocalDate.parse(from), LocalDate.parse(to))
              .returns(error)

            val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
            result shouldBe Left(ErrorWrapper(correlationId, mtdError, None))
          }
        }
    }

  }

}
