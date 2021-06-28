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

package v2.controllers

import java.time.LocalDate

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import v2.models.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.MockIdGenerator
import v2.mocks.requestParsers.MockEopsDeclarationRequestRawDataParser
import v2.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockEopsDeclarationRequestService, MockMtdIdLookupService}
import v2.models.audit.{AuditError, AuditEvent, EopsDeclarationAuditDetail, EopsDeclarationAuditResponse}
import v2.models.domain.EopsDeclarationSubmission
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.inbound.EopsDeclarationRawData
import v2.models.outcomes.DesResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EopsDeclarationControllerSpec extends ControllerBaseSpec
  with MockEopsDeclarationRequestService
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockEopsDeclarationRequestRawDataParser
  with MockAuditService
  with MockIdGenerator {


  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val requestJson = Json.parse(
    """
      |{
      |"finalised": true
      |}
    """.stripMargin)

  private val invalidRequestJson = Json.parse(
    """
      |{
      |"finalised": false
      |}
    """.stripMargin)


  class Test {
    MockIdGenerator.generateCorrelationId.returns(correlationId)
    MockedEnrolmentsAuthService.authoriseUser()
    MockedMtdIdLookupService.lookup("AA123456A")
      .returns(Future.successful(Right("test-mtd-id")))
    lazy val testController = new EopsDeclarationController(mockEnrolmentsAuthService,
      mockMtdIdLookupService,
      cc,
      mockIdGenerator,
      mockRequestDataParser,
      mockEopsDeclarationService,
      mockAuditService
    )
  }

  val nino: String = "AA123456A"
  val start: String = "2018-01-01"
  val end: String = "2018-12-31"
  val correlationId = "X-123"

  "Submit EOPS declaration" should {

    "return a 204 response" when {
      "a valid NINO, from and to date, with declaration as true is passed" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRawData = EopsDeclarationRawData(nino, start, end, AnyContentAsJson(requestJson))
        val eopsDeclarationSubmission: EopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))

        MockedEopsDeclarationService.submitDeclaration(eopsDeclarationSubmission)
          .returns(Future.successful(Right(DesResponse(correlationId, ()))))

        private val response: Future[Result] =
          testController.submit(nino, start, end)(fakePostRequest[JsValue](requestJson))

        status(response) shouldBe NO_CONTENT
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail: EopsDeclarationAuditDetail = EopsDeclarationAuditDetail("Agent", Some("agentId"), nino, start, end, requestJson, correlationId,
          EopsDeclarationAuditResponse(NO_CONTENT, None))
        val event: AuditEvent[EopsDeclarationAuditDetail] = AuditEvent("submitEndOfPeriodStatement", "uk-properties-submit-eops", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return a 403 (Forbidden) error" when {
      "a valid NINO, from and to date, with declaration as false is passed" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRawData = EopsDeclarationRawData(nino, start, end, AnyContentAsJson(invalidRequestJson))
        val eopsDeclarationSubmission: EopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))

        MockedEopsDeclarationService.submitDeclaration(eopsDeclarationSubmission)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, NotFinalisedDeclaration, None))))

        private val response: Future[Result] =
          testController.submit(nino, start, end)(fakePostRequest[JsValue](invalidRequestJson))

        status(response) shouldBe FORBIDDEN
        contentAsJson(response) shouldBe Json.toJson(NotFinalisedDeclaration)
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail: EopsDeclarationAuditDetail = EopsDeclarationAuditDetail("Agent", Some("agentId"), nino, start, end, invalidRequestJson, correlationId,
          EopsDeclarationAuditResponse(FORBIDDEN, Some(Seq(AuditError(NotFinalisedDeclaration.code)))))
        val event: AuditEvent[EopsDeclarationAuditDetail] = AuditEvent("submitEndOfPeriodStatement", "uk-properties-submit-eops", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }


    "return a error" when {
      "a valid NINO, start and end date, with no declaration is passed" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRawData = EopsDeclarationRawData(nino, start, end, AnyContentAsJson(Json.obj()))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Left(ErrorWrapper(correlationId, BadRequestError, None)))

        private val response: Future[Result] =
          testController.submit(nino, start, end)(fakeRequest.withBody(Json.obj()))

        status(response) shouldBe BAD_REQUEST
        contentAsJson(response) shouldBe Json.toJson(BadRequestError)
        header("X-CorrelationId", response) should not be empty

        val detail: EopsDeclarationAuditDetail = EopsDeclarationAuditDetail("Agent", Some("agentId"),
          nino, start, end, Json.obj(), header("X-CorrelationId", response).get,
          EopsDeclarationAuditResponse(BAD_REQUEST, Some(Seq(AuditError(BadRequestError.code)))))
        val event: AuditEvent[EopsDeclarationAuditDetail] = AuditEvent("submitEndOfPeriodStatement", "uk-properties-submit-eops", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }


    "return validation failed errors 400 (Bad Request)" when {

      val eopsErrors = Seq(
        InvalidStartDateError,
        InvalidEndDateError,
        RangeEndDateBeforeStartDateError,
        BadRequestError,
        NinoFormatError
      )

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

      val eopsErrors = Seq(NotFinalisedDeclaration, ConflictError, RuleClass4Over16, RuleClass4PensionAge,
        RuleFhlPrivateUseAdjustment, RuleNonFhlPrivateUseAdjustment,
        RuleMismatchStartDate, RuleMismatchEndDate, RuleConsolidatedExpenses, EarlySubmissionError, LateSubmissionError,
        NonMatchingPeriodError)

      for (error <- eopsErrors) {
        eopsDeclarationBusinessScenarios(error, FORBIDDEN)
      }
    }


    "return multiple errors 400 (Bad Request)" when {

      "validation is failed for more than one scenarios" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRawData = EopsDeclarationRawData(nino, start, end, AnyContentAsJson(invalidRequestJson))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(InvalidStartDateError, RangeEndDateBeforeStartDateError)))))

        private val response: Future[Result] =
          testController.submit(nino, start, end)(fakePostRequest[JsValue](invalidRequestJson))

        status(response) shouldBe BAD_REQUEST
        contentAsJson(response) shouldBe
          Json.toJson(ErrorWrapper(correlationId, BadRequestError,
            Some(Seq(InvalidStartDateError, RangeEndDateBeforeStartDateError))))
        header("X-CorrelationId", response) should not be empty

        val detail: EopsDeclarationAuditDetail = EopsDeclarationAuditDetail("Agent", Some("agentId"),
          nino, start, end, invalidRequestJson, header("X-CorrelationId", response).get,
          EopsDeclarationAuditResponse(BAD_REQUEST, Some(Seq(AuditError(InvalidStartDateError.code), AuditError(RangeEndDateBeforeStartDateError.code)))))
        val event: AuditEvent[EopsDeclarationAuditDetail] = AuditEvent("submitEndOfPeriodStatement", "uk-properties-submit-eops", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return multiple bvr errors 403 (Forbidden)" when {

      "business validation is failed for more than one scenarios" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRawData = EopsDeclarationRawData(nino, start, end, AnyContentAsJson(requestJson))
        val eopsDeclarationSubmission: EopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))

        MockedEopsDeclarationService.submitDeclaration(eopsDeclarationSubmission)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, BVRError, Some(Seq(RuleClass4Over16, RuleClass4PensionAge))))))

        private val response: Future[Result] =
          testController.submit(nino, start, end)(fakePostRequest[JsValue](requestJson))

        status(response) shouldBe FORBIDDEN
        contentAsJson(response) shouldBe Json.toJson(ErrorWrapper(correlationId, BVRError, Some(Seq(RuleClass4Over16, RuleClass4PensionAge))))
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail: EopsDeclarationAuditDetail = EopsDeclarationAuditDetail("Agent", Some("agentId"), nino, start, end, requestJson, correlationId,
          EopsDeclarationAuditResponse(FORBIDDEN, Some(Seq(AuditError(RuleClass4Over16.code), AuditError(RuleClass4PensionAge.code)))))
        val event: AuditEvent[EopsDeclarationAuditDetail] = AuditEvent("submitEndOfPeriodStatement", "uk-properties-submit-eops", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return a single error with 403 (Forbidden)" when {
      "business validation has failed with just one error" in new Test {

        val eopsDeclarationRequestData: EopsDeclarationRawData = EopsDeclarationRawData(nino, start, end, AnyContentAsJson(requestJson))
        val eopsDeclarationSubmission: EopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))

        MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
          .returns(Right(eopsDeclarationSubmission))

        MockedEopsDeclarationService.submitDeclaration(eopsDeclarationSubmission)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleClass4Over16, None))))

        private val response: Future[Result] =
          testController.submit(nino, start, end)(fakePostRequest[JsValue](requestJson))

        status(response) shouldBe FORBIDDEN
        contentAsJson(response) shouldBe Json.toJson(ErrorWrapper(correlationId, RuleClass4Over16, None))
        header("X-CorrelationId", response) shouldBe Some(correlationId)

        val detail: EopsDeclarationAuditDetail = EopsDeclarationAuditDetail("Agent", Some("agentId"), nino, start, end, requestJson, correlationId,
          EopsDeclarationAuditResponse(FORBIDDEN, Some(Seq(AuditError(RuleClass4Over16.code)))))
        val event: AuditEvent[EopsDeclarationAuditDetail] = AuditEvent("submitEndOfPeriodStatement", "uk-properties-submit-eops", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }
  }

  def eopsDeclarationValidationScenarios(error: Error, expectedStatus: Int): Unit = {
    s"returned a ${error.code} error" in new Test {

      val eopsDeclarationRequestData: EopsDeclarationRawData = EopsDeclarationRawData(nino, start, end, AnyContentAsJson(requestJson))
      val eopsDeclarationSubmission: EopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))

      MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
        .returns(Right(eopsDeclarationSubmission))

      MockedEopsDeclarationService.submitDeclaration(eopsDeclarationSubmission)
        .returns(Future.successful(Left(ErrorWrapper(correlationId, error, None))))

      val response: Future[Result] = testController.submit(nino, start, end)(fakePostRequest[JsValue](requestJson))
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

      val detail: EopsDeclarationAuditDetail = EopsDeclarationAuditDetail("Agent", Some("agentId"), nino, start, end, requestJson, correlationId,
        EopsDeclarationAuditResponse(expectedStatus, Some(Seq(AuditError(error.code)))))
      val event: AuditEvent[EopsDeclarationAuditDetail] = AuditEvent("submitEndOfPeriodStatement", "uk-properties-submit-eops", detail)
      MockedAuditService.verifyAuditEvent(event).once
    }
  }


  def eopsDeclarationBusinessScenarios(error: Error, expectedStatus: Int): Unit = {
    s"returned a ${error.code} error" in new Test {

      val eopsDeclarationRequestData: EopsDeclarationRawData = EopsDeclarationRawData(nino, start, end, AnyContentAsJson(requestJson))
      val eopsDeclarationSubmission: EopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), LocalDate.parse(start), LocalDate.parse(end))

      MockedEopsDeclarationRequestDataParser.parseRequest(eopsDeclarationRequestData)
        .returns(Right(eopsDeclarationSubmission))

      MockedEopsDeclarationService.submitDeclaration(EopsDeclarationSubmission(Nino(nino),
        LocalDate.parse(start), LocalDate.parse(end)))
        .returns(Future.successful(Left(ErrorWrapper(correlationId, error, None))))

      val response: Future[Result] = testController.submit(nino, start, end)(fakePostRequest[JsValue](requestJson))
      status(response) shouldBe expectedStatus
      contentAsJson(response) shouldBe Json.toJson(error)
      header("X-CorrelationId", response) shouldBe Some(correlationId)

      val detail: EopsDeclarationAuditDetail = EopsDeclarationAuditDetail("Agent", Some("agentId"), nino, start, end, requestJson, correlationId,
        EopsDeclarationAuditResponse(expectedStatus, Some(Seq(AuditError(error.code)))))
      val event: AuditEvent[EopsDeclarationAuditDetail] = AuditEvent("submitEndOfPeriodStatement", "uk-properties-submit-eops", detail)
      MockedAuditService.verifyAuditEvent(event).once
    }
  }

}
