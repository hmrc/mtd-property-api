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

import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.errors.Error
import v2.models.{FulfilledObligation, Obligation, ObligationDetails, outcomes}

import scala.concurrent.{ExecutionContext, Future}

class EopsObligationsServiceSpec extends ServiceSpec {


  class Test {
    val mockDesConnector: DesConnector = mock[DesConnector]
    val service = new EopsObligationsService(mockDesConnector)
  }

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

  val emptyEopsObligations: Seq[Obligation] = Seq()


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
        val outcomesObligations: Future[Either[Seq[Error], Seq[ObligationDetails]]] = Future(Right(validObligationsData))

        (mockDesConnector.getObligations(_: String, _: LocalDate, _: LocalDate)(_:HeaderCarrier, _:ExecutionContext))
          .expects(*, *, *, *, *)
          .returns(outcomesObligations)

        val from: LocalDate = LocalDate.parse("2018-01-01")
        val to: LocalDate = LocalDate.parse("2018-12-31")

        val result: Either[Seq[Error], Seq[Obligation]] = await(service.retrieveEopsObligations("AA123456A", from, to))
        result shouldBe Right(successEopsObligations)
      }
    }
    "return empty obligations details" when {
      "no ISTP EOPS obligation types are returned" in new Test {
        val outcomesObligations: Future[Either[Seq[Error], Seq[ObligationDetails]]] = Future(Right(emptyObligationsData))

        (mockDesConnector.getObligations(_: String, _: LocalDate, _: LocalDate)(_:HeaderCarrier, _:ExecutionContext))
          .expects(*, *, *, *, *)
          .returns(outcomesObligations)

        val from: LocalDate = LocalDate.parse("2018-01-01")
        val to: LocalDate = LocalDate.parse("2018-12-31")

        val result: Either[Seq[Error], Seq[Obligation]] = await(service.retrieveEopsObligations("AA123456A", from, to))
        result shouldBe Right(emptyEopsObligations)
      }
    }
  }

}
