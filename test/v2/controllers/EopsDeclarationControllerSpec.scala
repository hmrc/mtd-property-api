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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.services.{MockEnrolmentsAuthService, MockEopsDeclarationService, MockMtdIdLookupService}
import v2.models.errors._
import v2.models.errors.SubmitEopsDeclarationErrors._

import scala.concurrent.Future

class EopsDeclarationControllerSpec extends ControllerBaseSpec
  with MockEopsDeclarationService
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService {


  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val requestJson =
    """
      |{
      |"finalised": true
      |}
    """.stripMargin

  /*private val invalidRequestJson =
    """
      |{
      |"finalised": false
      |}
    """.stripMargin

  private val invalidNullRequestJson =
    """
      |{
      |"finalised": null
      |}
    """.stripMargin*/

  class Test {
    MockedEnrolmentsAuthService.authoriseUser()
    MockedMtdIdLookupService.lookup("AA123456A")
      .returns(Future.successful(Right("test-mtd-id")))
    lazy val testController = new EopsDeclarationController(mockEnrolmentsAuthService,
      mockMtdIdLookupService,
      mockEopsDeclarationService)
  }

  val nino: String = "AA123456A"
  val from: String = "2018-01-01"
  val to: String = "2018-12-31"

  "Submit EOPS declaration" should {

    "return a 204 response" when {
      "a valid NINO, from and to date, and declaration is passed" in new Test {

        MockedEopsDeclarationService.submitDeclaration(nino, from, to)
          .returns(Future.successful(None))

        private val response: Future[Result] =
          testController.submit(nino, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))

        status(response) shouldBe NO_CONTENT
      }
    }

    "return error 400 (Bad Request)" when {

      val eopsErrors = Seq(
        InvalidStartDateError, InvalidEndDateError, InvalidRangeError, BadRequestError,
        InvalidNinoError, EarlySubmissionError, LateSubmissionError)

      for (error <- eopsErrors){
        eopsDeclarationErrorScenarios(error, BAD_REQUEST)
      }
    }

    "return a 404 (Not Found) error" when {
      eopsDeclarationErrorScenarios(NotFoundError, NOT_FOUND)
    }

    "return a 500 (ISE)" when {
      eopsDeclarationErrorScenarios(DownstreamError, INTERNAL_SERVER_ERROR)
    }

    "return error 403 (Forbidden)" when {

      val eopsErrors = Seq(ConflictError, RuleClass4Over16, RuleClass4PensionAge,
        RuleFhlPrivateUseAdjustment, RuleNonFhlPrivateUseAdjustment,
        RuleMismatchStartDate, RuleMismatchEndDate)

      for (error <- eopsErrors) {
        eopsDeclarationErrorScenarios(error, FORBIDDEN)
      }
    }
  }

  def eopsDeclarationErrorScenarios(error: v2.models.errors.Error, expectedStatus: Int): Unit =
  {
    s"returned a ${error.code} error" in new Test {
      MockedEopsDeclarationService.submitDeclaration(nino, from, to)
        .returns(Future.successful(Some(ErrorResponse(error, None))))

      val response: Future[Result] = testController.submit(nino, from, to)(fakePostRequest[JsValue](Json.parse(requestJson)))
      status(response) shouldBe expectedStatus
    }
  }
}
