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

package v2.controllers

import java.time.LocalDate

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.services.{MockEnrolmentsAuthService, MockEopsObligationsService, MockMtdIdLookupService}
import v2.models.errors.ErrorResponse
import v2.models.errors.GetEopsObligationsErrors.InvalidToDateError
import v2.models.{FulfilledObligation, Obligation}

import scala.concurrent.Future

class EopsObligationsControllerSpec extends ControllerBaseSpec
  with MockEopsObligationsService
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService {


  implicit val hc: HeaderCarrier = HeaderCarrier()

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

  class Test {
    MockedEnrolmentsAuthService.authoriseUser()
    MockedMtdIdLookupService.lookup("AA123456A")
      .returns(Future.successful(Right("test-mtd-id")))
    lazy val testController = new EopsObligationsController(mockEnrolmentsAuthService,
      mockMtdIdLookupService,
      mockEopsObligationsService)
  }

  val nino: String = "AA123456A"
  val from: String = "2018-01-01"
  val to: String = "2018-12-31"

  "GET EOPS Obligations controller" should {
    
    "return valid EOPS Obligations" when {
      "passed a valid NINO, from and to date" in new Test {
        MockedEopsObligationsService.retrieveEopsObligations(nino, from, to)
          .returns(Future.successful(Right(successEopsObligations)))
        val response: Future[Result] = testController.getEopsObligations(nino, from, to)(fakeRequest)
        status(response) shouldBe OK
        contentAsJson(response) shouldBe Json.toJson(successEopsObligations)
      }
    }

    "return error BAD REQUEST" when {
      "passed invalid parameters" in new Test {
        MockedEopsObligationsService.retrieveEopsObligations(nino, from, to)
          .returns(Future.successful(Left(ErrorResponse(InvalidToDateError, None))))
        val response: Future[Result] = testController.getEopsObligations(nino, from, to)(fakeRequest)
        status(response) shouldBe BAD_REQUEST
      }
    }
  }
}
