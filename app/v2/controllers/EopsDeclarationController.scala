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

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v2.controllers.requestParsers.EopsDeclarationRequestDataParser
import v2.models.audit.{AuditError, AuditEvent, EopsDeclarationAuditDetail, EopsDeclarationAuditResponse}
import v2.models.auth.UserDetails
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.inbound.EopsDeclarationRawData
import v2.services.{AuditService, EnrolmentsAuthService, EopsDeclarationService, MtdIdLookupService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EopsDeclarationController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          val cc: ControllerComponents,
                                          requestDataParser: EopsDeclarationRequestDataParser,
                                          service: EopsDeclarationService,
                                          auditService: AuditService)(implicit ec: ExecutionContext) extends AuthorisedController(cc) {

  val logger: Logger = Logger(this.getClass)

  def submit(nino: String, start: String, end: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      requestDataParser.parseRequest(EopsDeclarationRawData(nino, start, end, AnyContentAsJson(request.body))) match {
        case Right(eopsDeclarationSubmission) =>
          service.submit(eopsDeclarationSubmission).map {
            case Right(desResponse) =>
              auditSubmission(createAuditDetails(nino, start, end, NO_CONTENT, request.body, desResponse.correlationId, request.userDetails, None))
              NoContent.withHeaders("X-CorrelationId" -> desResponse.correlationId)
            case Left(errorWrapper) =>
              val correlationId = getCorrelationId(errorWrapper)
              val result = processError(errorWrapper)
              auditSubmission(createAuditDetails(nino, start, end, result.header.status, request.body, correlationId, request.userDetails, Some(errorWrapper)))
              result.withHeaders("X-CorrelationId" -> correlationId)
          }
        case Left(errorWrapper) =>
          val correlationId = getCorrelationId(errorWrapper)
          val result = processError(errorWrapper)
          auditSubmission(createAuditDetails(nino, start, end, result.header.status, request.body, correlationId, request.userDetails, Some(errorWrapper)))
          Future.successful(result.withHeaders("X-CorrelationId" -> correlationId))
      }
    }

  private def processError(errorResponse: ErrorWrapper) = {
    errorResponse.error match {
      case InvalidStartDateError
           | InvalidEndDateError
           | RangeToDateBeforeFromDateError
           | RangeEndDateBeforeStartDateError
           | BadRequestError
           | NinoFormatError =>
        BadRequest(Json.toJson(errorResponse))
      case ConflictError
           | NotFinalisedDeclaration
           | RuleClass4Over16
           | RuleClass4PensionAge
           | RuleFhlPrivateUseAdjustment
           | RuleNonFhlPrivateUseAdjustment
           | RuleMismatchStartDate
           | RuleMismatchEndDate
           | RuleConsolidatedExpenses
           | EarlySubmissionError
           | LateSubmissionError
           | BVRError =>
        Forbidden(Json.toJson(errorResponse))
      case NotFoundError => NotFound(Json.toJson(errorResponse))
      case DownstreamError => InternalServerError(Json.toJson(errorResponse))
    }
  }

  private def getCorrelationId(errorWrapper: ErrorWrapper): String = {
    errorWrapper.correlationId match {
      case Some(correlationId) => logger.info("[EopsDeclarationController][getCorrelationId] - " +
        s"Error received from DES ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
      case None =>
        val correlationId = UUID.randomUUID().toString
        logger.info("[EopsDeclarationController][getCorrelationId] - " +
          s"Validation error: ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
    }
  }

  private def createAuditDetails(nino: String,
                                 start: String,
                                 end: String,
                                 statusCode: Int,
                                 request: JsValue,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 errorWrapper: Option[ErrorWrapper] = None
                                ): EopsDeclarationAuditDetail = {
    val response = errorWrapper.map {
      wrapper =>
        EopsDeclarationAuditResponse(statusCode, Some(wrapper.allErrors.map(error => AuditError(error.code))))
    }.getOrElse(EopsDeclarationAuditResponse(statusCode, None))

    EopsDeclarationAuditDetail(
      userType = userDetails.userType,
      agentReferenceNumber = userDetails.agentReferenceNumber,
      nino = nino,
      from = start,
      to = end,
      request = request,
      `X-CorrelationId` = correlationId,
      response = response)
  }

  private def auditSubmission(details: EopsDeclarationAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("submitEndOfPeriodStatement", "uk-properties-submit-eops", details)
    auditService.auditEvent(event)
  }
}
