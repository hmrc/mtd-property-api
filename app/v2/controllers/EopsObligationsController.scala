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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import v2.models.errors.GetEopsObligationsErrors._
import v2.models.errors._
import v2.services.{EnrolmentsAuthService, EopsObligationsService, MtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class EopsObligationsController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          val service: EopsObligationsService) extends AuthorisedController {

  def getEopsObligations(nino: String, from: String, to: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      service.retrieveEopsObligations(nino, from, to).map {
        case Left(e) => processError(e)
        case Right(success) => Ok(Json.obj("obligations" -> Json.toJson(success)))
      }
    }

  private def processError(errorResponse: ErrorWrapper) = {
    errorResponse.error match {
      case MissingFromDateError | MissingToDateError
           | InvalidFromDateError | InvalidToDateError
           | InvalidRangeErrorGetEops | RangeTooBigError | InvalidRangeErrorGetEops // TODO CHeck if we can del last
           | BadRequestError | NinoFormatError =>
        BadRequest(Json.toJson(errorResponse))
      case NotFoundError => NotFound(Json.toJson(errorResponse))
      case DownstreamError => InternalServerError(Json.toJson(errorResponse))
    }
  }
}