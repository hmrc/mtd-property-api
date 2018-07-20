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

package v2.models

import java.time.LocalDate

import support.UnitSpec

import scala.util.{Failure, Success, Try}

class ObligationSpec extends UnitSpec {

  val start: LocalDate = LocalDate.parse("2017-04-06")
  val end: LocalDate = LocalDate.parse("2018-04-05")
  val due: LocalDate = LocalDate.parse("2019-01-31")
  val statusFulfilled: FulfilledObligation.type = FulfilledObligation
  val statusOpen: ObligationStatus = OpenObligation
  val processed = Some(LocalDate.parse("2018-05-01"))
  val periodKey: String = ""

  "Creating an obligation with valid details" should {

    val obligation = Obligation(
      startDate = start,
      endDate = end,
      dueDate = due,
      status = statusFulfilled,
      processedDate = processed,
      periodKey = periodKey
    )

    "result in an obligation with the correct start date" in {
      obligation.startDate shouldBe start
    }

    "result in an obligation with the correct end date" in {
      obligation.endDate shouldBe end
    }

    "result in an obligation with the correct due date" in {
      obligation.dueDate shouldBe due
    }

    "result in an obligation with the correct status" in {
      obligation.status shouldBe statusFulfilled
    }

    "result in an obligation with the correct processed date" in {
      obligation.processedDate shouldBe processed
    }

    "result in an obligation with the correct period key" in {
      obligation.periodKey shouldBe periodKey
    }
  }

  "Creating an fulfilled obligation without a processed date" should {
    "throw an exception" in {

      Try(
        Obligation(
          startDate = start,
          endDate = end,
          dueDate = due,
          status = statusFulfilled,
          processedDate = None,
          periodKey = periodKey
        )
      ) match {
        case Success(_) => fail("Fulfilled obligation must have a processed date")
        case Failure(e) => e.getMessage shouldBe "Cannot create a fulfilled obligation without a processed date"
      }

    }
  }

  "Creating an open obligation with a processed date" should {
    "throw an exception" in {

      Try(
        Obligation(
          startDate = start,
          endDate = end,
          dueDate = due,
          status = statusOpen,
          processedDate = processed,
          periodKey = periodKey
        )
      ) match {
        case Success(_) => fail("Open obligation must NOT have a processed date")
        case Failure(e) => e.getMessage shouldBe "Cannot create an open obligation with a processed date"
      }

    }
  }

}
