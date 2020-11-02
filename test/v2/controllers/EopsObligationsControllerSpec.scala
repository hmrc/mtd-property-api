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

package v2.controllers

import java.time.LocalDate

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.MockIdGenerator
import v2.mocks.services.{MockEnrolmentsAuthService, MockEopsObligationsService, MockMtdIdLookupService}
import v2.models.domain.{FulfilledObligation, Obligation}
import v2.models.errors.GetEopsObligationsErrors._
import v2.models.errors._
import v2.models.outcomes.DesResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EopsObligationsControllerSpec extends ControllerBaseSpec
  with MockEopsObligationsService
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockIdGenerator {


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
    MockIdGenerator.generateCorrelationId.returns(correlationId)
    MockedEnrolmentsAuthService.authoriseUser()
    MockedMtdIdLookupService.lookup("AA123456A")
      .returns(Future.successful(Right("test-mtd-id")))
    lazy val testController = new EopsObligationsController(mockEnrolmentsAuthService,
      mockMtdIdLookupService,
      cc,
      mockIdGenerator,
      mockEopsObligationsService)
  }

  val nino: String = "AA123456A"
  val from: String = "2018-01-01"
  val to: String = "2018-12-31"
  val correlationId = "X-123"

  def eopsErrorTest(error: v2.models.errors.Error, expectedStatus: Int): Unit =
    {
      s"returned a ${error.code} error" in new Test {
        MockedEopsObligationsService.retrieveEopsObligations(nino, from, to)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, error, None))))

        val response: Future[Result] = testController.getEopsObligations(nino, from, to)(fakeRequest)
        status(response) shouldBe expectedStatus
        header("X-CorrelationId", response) should not be empty
      }
    }

  "GET EOPS Obligations controller" should {

    "return a 200 response" when {
      "passed a valid NINO, from and to date" in new Test {

        MockedEopsObligationsService.retrieveEopsObligations(nino, from, to)
          .returns(Future.successful(Right(DesResponse(correlationId, successEopsObligations))))

        private val expectedJson = Json.obj("obligations" -> Json.toJson(successEopsObligations))
        private val response: Future[Result] = testController.getEopsObligations(nino, from, to)(fakeRequest)

        status(response) shouldBe OK
        contentAsJson(response) shouldBe expectedJson
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }

    "return valid EOPS Obligations" when {
      "passed a valid NINO, from and to date" in new Test {

        MockedEopsObligationsService.retrieveEopsObligations(nino, from, to)
          .returns(Future.successful(Right(DesResponse(correlationId, successEopsObligations))))

        private val expectedJson = Json.obj("obligations" -> Json.toJson(successEopsObligations))
        private val response: Future[Result] = testController.getEopsObligations(nino, from, to)(fakeRequest)

        contentAsJson(response) shouldBe expectedJson
        header("X-CorrelationId", response) shouldBe Some(correlationId)
      }
    }

    "return error 400 (Bad Request)" when {

      val eopsErrors = Seq(
        MissingFromDateError, MissingToDateError,
        InvalidFromDateError, InvalidToDateError,
        RangeEndDateBeforeStartDateError, RangeTooBigError,
        BadRequestError, NinoFormatError)

      for (error <- eopsErrors){
        eopsErrorTest(error, BAD_REQUEST)
      }
    }

    "return a 404 (Not Found) error" when {
      eopsErrorTest(NotFoundError, NOT_FOUND)
    }

    "return a 500 (ISE)" when {
      eopsErrorTest(DownstreamError, INTERNAL_SERVER_ERROR)
    }
  }
}
