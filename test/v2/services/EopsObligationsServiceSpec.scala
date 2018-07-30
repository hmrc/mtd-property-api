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
import v2.models.errors.{ErrorResponse, InvalidNinoError, NotFoundError}
import v2.models.outcomes.{EopsObligationsOutcome, ObligationsOutcome}
import v2.models.{FulfilledObligation, Obligation, ObligationDetails}

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

  "calling retrieveEopsObligations" should {
    "return multiple obligations details" when {
      "successful request is made to des" in new Test {
        val nino: String = "AA123456A"
        val from: String = "2018-01-01"
        val to: String = "2018-12-31"

        val outcomesObligations: Future[ObligationsOutcome] = Future(Right(validObligationsData))

        MockedDesConnector.getObligations(nino, LocalDate.parse(from), LocalDate.parse(to))
          .returns(outcomesObligations)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Right(successEopsObligations)
      }
    }

    "return Not Found error" when {
      "no ISTP EOPS obligation types are returned" in new Test {
        val nino: String = "AA123456A"
        val from: String = "2018-01-01"
        val to: String = "2018-12-31"

        val outcomesObligations: Future[ObligationsOutcome] = Future(Right(validNonEopsObligationsData))

        MockedDesConnector.getObligations(nino, LocalDate.parse(from), LocalDate.parse(to))
          .returns(outcomesObligations)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(ErrorResponse(NotFoundError, None))
      }
    }

    "return an invalid NINO error" when {
      "the NINO is in the wrong format" in new Test {
        val nino: String = "BOB"
        val from: String = "2018-01-01"
        val to: String = "2018-12-31"

        val expectedErrorResponse = ErrorResponse(InvalidNinoError, None)

        val result: EopsObligationsOutcome = await(service.retrieveEopsObligations(nino, from, to))
        result shouldBe Left(expectedErrorResponse)
      }
    }

    "return a missing from date error" when {
      "the from date is None" in new Test {
        pending
      }
    }

    "return an invalid from date error" when {
      "the from date is in the wrong format" in new Test {
        pending
      }
    }

    "return a missing to date error" when {
      "the to date is None" in new Test {
        pending
      }
    }

    "return invalid to date error" when {
      "the to date is in the wrong format" in new Test {
        pending
      }
    }

    "return multiple errors" when {
      "the there are problems with more than one input" in new Test {
        pending
      }
    }
  }

}