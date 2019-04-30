/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContentAsJson}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v2.controllers.requestParsers.EopsDeclarationRequestDataParser
import v2.models.audit.{AuditError, AuditEvent, EopsDeclarationAuditDetail, EopsDeclarationAuditResponse}
import v2.models.auth.UserDetails
import v2.models.domain.EopsDeclarationSubmission
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.inbound.EopsDeclarationRawData
import v2.services.{AuditService, EnrolmentsAuthService, EopsDeclarationService, MtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EopsDeclarationController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          val requestDataParser: EopsDeclarationRequestDataParser,
                                          val service: EopsDeclarationService,
                                          auditService: AuditService) extends AuthorisedController {

  val logger: Logger = Logger(this.getClass)

  def submit(nino: String, start: String, end: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      implicit val userDetails: UserDetails = request.userDetails
      requestDataParser.parseRequest(EopsDeclarationRawData(nino, start, end, AnyContentAsJson(request.body))) match {
        case Right(eopsDeclarationSubmission) =>
          service.submit(eopsDeclarationSubmission).map {
            case Right(desResponse) =>
              auditSubmission(createAuditDetails(NO_CONTENT, desResponse.correlationId, userDetails, true, eopsDeclarationSubmission, None))
              NoContent

            case Left(errorWrapper) =>
//              val correlationId = getCorrelationId(errorWrapper)
//              auditSubmission(createAuditDetails(NO_CONTENT, correlationId, userDetails, true, eopsDeclarationSubmission, Some(errorWrapper)))
              processError(errorWrapper)
          }
        case Left(validationErrorResponse) => Future {
          processError(validationErrorResponse)
        }
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
      ???
//      errorWrapper.correlationId match {
//        case Some(correlationId) => logger.info("[EopsDeclarationController][getCorrelationId] - " +
//          s"Error received from DES ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
//          correlationId
//        case None =>
//          val correlationId = UUID.randomUUID().toString
//          logger.info("[EopsDeclarationController][getCorrelationId] - " +
//            s"Validation error: ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
//          correlationId
//      }
    }

  private def createAuditDetails(
                                  statusCode: Int,
                                  correlationId: String,
                                  userDetails: UserDetails,
                                  finalised: Boolean,
                                  submission: EopsDeclarationSubmission,
                                  errorWrapper: Option[ErrorWrapper] = None
                                ): EopsDeclarationAuditDetail = {
    val response = errorWrapper.map {
      wrapper =>
        EopsDeclarationAuditResponse(statusCode, Some(wrapper.allErrors.map(error => AuditError(error.code))))
    }.getOrElse(EopsDeclarationAuditResponse(statusCode, None))

    EopsDeclarationAuditDetail(
      userType = userDetails.userType,
      agentReferenceNumber = userDetails.agentReferenceNumber,
      nino = submission.nino.toString,
      from = submission.start.toString,
      to = submission.end.toString,
      finalised = finalised,
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
