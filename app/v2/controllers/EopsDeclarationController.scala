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

import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import v2.controllers.validators.EopsDeclarationValidator
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.services.{EnrolmentsAuthService, EopsDeclarationService, MtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EopsDeclarationController @Inject()(val authService: EnrolmentsAuthService,
                                         val lookupService: MtdIdLookupService,
                                         val validator: EopsDeclarationValidator,
                                         val service: EopsDeclarationService) extends AuthorisedController {

  def submit(nino: String, from: String, to: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      validator.validateSubmit(nino, from, to, request.body) match {
        case Left(errorResponse) => Future {processError(errorResponse)}
        case Right(eopsDeclarationSubmission) =>
          service.submit(eopsDeclarationSubmission).map {
            case None => NoContent
            case Some(result) => processError(result)
          }
      }
    }

  private def processError(errorResponse: ErrorResponse) = {
    errorResponse.error match {
      case InvalidStartDateError
           | InvalidEndDateError
           | InvalidRangeError
           | BadRequestError
           | InvalidNinoError
           | EarlySubmissionError
           | LateSubmissionError =>
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
           | BVRError =>
        Forbidden(Json.toJson(errorResponse))
      case NotFoundError => NotFound(Json.toJson(errorResponse))
      case DownstreamError => InternalServerError(Json.toJson(errorResponse))
    }
  }
}
