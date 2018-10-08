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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.controllers.validators.EopsDeclarationSubmission
import v2.mocks.services.{MockEnrolmentsAuthService, MockEopsDeclarationService, MockMtdIdLookupService}
import v2.mocks.validators.MockEopsDeclarationValidator
import v2.models.errors._
import v2.models.errors.SubmitEopsDeclarationErrors._

import scala.concurrent.Future

class EopsDeclarationControllerSpec extends ControllerBaseSpec
  with MockEopsDeclarationService
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockEopsDeclarationValidator {


  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val requestJson =
    """
      |{
      |"finalised": true
      |}
    """.stripMargin

  private val invalidRequestJson =
    """
      |{
      |"finalised": false
      |}
    """.stripMargin


  class Test {
    MockedEnrolmentsAuthService.authoriseUser()
    MockedMtdIdLookupService.lookup("AA123456A")
      .returns(Future.successful(Right("test-mtd-id")))
    lazy val testController = new EopsDeclarationController(mockEnrolmentsAuthService,
      mockMtdIdLookupService,
      mockEopsDeclarationValidator,
      mockEopsDeclarationService)
  }

  val nino: String = "AA123456A"
  val from: String = "2018-01-01"
  val to: String = "2018-12-31"

  "Submit EOPS declaration" should {

    "return a 204 response" when {
      "a valid NINO, from and to date, with declaration as true is passed" in new Test {

        MockEopsDeclarationValidator.validateSubmit(nino, from, to, Json.parse(requestJson))
          .returns(Right(EopsDeclarationSubmission(Nino(nino),
            LocalDate.parse(from), LocalDate.parse(to))))

        MockedEopsDeclarationService.submitDeclaration(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(from), LocalDate.parse(to)))
          .returns(Future.successful(None))

        private val response: Future[Result] =
          testController.submit(nino, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))

        status(response) shouldBe NO_CONTENT
      }
    }

    "return a 400 (Bad Request) error" when {
      "a valid NINO, from and to date, with declaration as false is passed" in new Test {

        MockEopsDeclarationValidator.validateSubmit(nino, from, to, Json.parse(invalidRequestJson))
          .returns(Left(ErrorResponse(NotFinalisedDeclaration, None)))

        private val response: Future[Result] =
          testController.submit(nino, from, to)(fakePostRequest[JsValue](Json.parse(invalidRequestJson)))

        status(response) shouldBe BAD_REQUEST
        contentAsJson(response) shouldBe Json.toJson(NotFinalisedDeclaration)
      }
    }

    "return a error" when {
      "a valid NINO, from and to date, with no declaration is passed" in new Test {

        MockEopsDeclarationValidator.validateSubmit(nino, from, to, Json.parse("""{}""".stripMargin))
          .returns(Left(ErrorResponse(NotFinalisedDeclaration, None)))

        private val response: Future[Result] =
          testController.submit(nino, from, to)(fakeRequest.withBody(Json.parse("""{}""".stripMargin)))

        status(response) shouldBe BAD_REQUEST
        contentAsJson(response) shouldBe Json.toJson(NotFinalisedDeclaration)
      }
    }

    "return validation failed errors 400 (Bad Request)" when {

      val eopsErrors = Seq(InvalidStartDateError, InvalidEndDateError,
        InvalidRangeError, BadRequestError,
        InvalidNinoError)

      for (error <- eopsErrors) {
        eopsDeclarationValidationScenarios(error, BAD_REQUEST)
      }
    }

    "return business failed errors 400 (Bad Request)" when {

      val eopsErrors = Seq(EarlySubmissionError, LateSubmissionError)

      for (error <- eopsErrors) {
        eopsDeclarationValidationScenarios(error, BAD_REQUEST)
      }
    }

    "return a 404 (Not Found) error" when {
      eopsDeclarationBusinessScenarios(NotFoundError, NOT_FOUND)
    }

    "return a 500 (ISE)" when {
      eopsDeclarationBusinessScenarios(DownstreamError, INTERNAL_SERVER_ERROR)
    }

    "return error 403 (Forbidden)" when {

      val eopsErrors = Seq(ConflictError, RuleClass4Over16, RuleClass4PensionAge,
        RuleFhlPrivateUseAdjustment, RuleNonFhlPrivateUseAdjustment,
        RuleMismatchStartDate, RuleMismatchEndDate, RuleConsolidatedExpenses)

      for (error <- eopsErrors) {
        eopsDeclarationBusinessScenarios(error, FORBIDDEN)
      }
    }

    "return multiple errors 400 (Bad Request)" when {

      "validation is failed for more than one scenarios" in new Test {

        MockEopsDeclarationValidator.validateSubmit(nino, from, to, Json.parse(invalidRequestJson))
          .returns(Left(ErrorResponse(BadRequestError, Some(Seq(InvalidStartDateError, InvalidRangeError)))))

        private val response: Future[Result] =
          testController.submit(nino, from, to)(fakePostRequest[JsValue](Json.parse(invalidRequestJson)))

        status(response) shouldBe BAD_REQUEST
        contentAsJson(response) shouldBe Json.toJson(ErrorResponse(BadRequestError, Some(Seq(InvalidStartDateError, InvalidRangeError))))
      }
    }

    "return multiple bvr errors 403 (Forbidden)" when {

      "business validation is failed for more than one scenarios" in new Test {

        MockEopsDeclarationValidator.validateSubmit(nino, from, to, Json.parse(requestJson))
          .returns(Right(EopsDeclarationSubmission(Nino(nino),
            LocalDate.parse(from), LocalDate.parse(to))))

        MockedEopsDeclarationService.submitDeclaration(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(from), LocalDate.parse(to)))
          .returns(Future.successful(Some(ErrorResponse(BVRError, Some(Seq(RuleClass4Over16, RuleClass4PensionAge))))))

        private val response: Future[Result] =
          testController.submit(nino, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))

        status(response) shouldBe FORBIDDEN
        contentAsJson(response) shouldBe Json.toJson(ErrorResponse(BVRError, Some(Seq(RuleClass4Over16, RuleClass4PensionAge))))
      }
    }
  }

  def eopsDeclarationValidationScenarios(error: v2.models.errors.Error, expectedStatus: Int): Unit =
  {
    s"returned a ${error.code} error" in new Test {

      MockEopsDeclarationValidator.validateSubmit(nino, from, to, Json.parse(requestJson))
        .returns(Left(ErrorResponse(error, None)))

      val response: Future[Result] = testController.submit(nino, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
    }
  }

  def eopsDeclarationBusinessScenarios(error: v2.models.errors.Error, expectedStatus: Int): Unit =
  {
    s"returned a ${error.code} error" in new Test {

      MockEopsDeclarationValidator.validateSubmit(nino, from, to, Json.parse(requestJson))
        .returns(Right(EopsDeclarationSubmission(Nino(nino),
          LocalDate.parse(from), LocalDate.parse(to))))

      MockedEopsDeclarationService.submitDeclaration(EopsDeclarationSubmission(Nino(nino),
        LocalDate.parse(from), LocalDate.parse(to)))
        .returns(Future.successful(Some(ErrorResponse(error, None))))

      val response: Future[Result] = testController.submit(nino, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
    }
  }
}
