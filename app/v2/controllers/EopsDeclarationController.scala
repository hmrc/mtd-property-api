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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson}
import v2.controllers.requestParsers.EopsDeclarationRequestDataParser
import v2.models.auth.UserDetails
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.models.inbound.EopsDeclarationRawData
import v2.services.{EnrolmentsAuthService, EopsDeclarationService, MtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EopsDeclarationController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          val requestDataParser: EopsDeclarationRequestDataParser,
                                          val service: EopsDeclarationService) extends AuthorisedController {

  def submit(nino: String, start: String, end: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      implicit val userDetails: UserDetails = request.userDetails
      requestDataParser.parseRequest(EopsDeclarationRawData(nino, start, end, AnyContentAsJson(request.body))) match {
        case Right(eopsDeclarationSubmission) =>
          service.submit(eopsDeclarationSubmission).map {
            case Right(_)  => NoContent
            case Left(errorResponse) => processError(errorResponse)
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
}
