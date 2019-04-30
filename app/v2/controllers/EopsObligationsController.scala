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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import v2.models.errors.GetEopsObligationsErrors._
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.services.{EnrolmentsAuthService, EopsObligationsService, MtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class EopsObligationsController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          val service: EopsObligationsService) extends AuthorisedController {

  val logger: Logger = Logger(this.getClass)

  def getEopsObligations(nino: String, from: String, to: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      service.retrieveEopsObligations(nino, from, to).map {
        case Left(e) =>
          processError(e).withHeaders("X-CorrelationId" -> getCorrelationId(e))
        case Right(DesResponse(correlationId, success)) =>
          Ok(Json.obj("obligations" -> Json.toJson(success))).withHeaders("X-CorrelationId" -> correlationId)
      }
    }

  private def processError(errorResponse: ErrorWrapper) = {
    errorResponse.error match {
      case MissingFromDateError | MissingToDateError
           | InvalidFromDateError | InvalidToDateError
           | RangeToDateBeforeFromDateError | RangeEndDateBeforeStartDateError | RangeTooBigError
           | BadRequestError | NinoFormatError =>
        BadRequest(Json.toJson(errorResponse))
      case NotFoundError => NotFound(Json.toJson(errorResponse))
      case DownstreamError => InternalServerError(Json.toJson(errorResponse))
    }
  }
  private def getCorrelationId(errorWrapper: ErrorWrapper): String = {
    errorWrapper.correlationId match {
      case Some(correlationId) => logger.info("[EopsObligationsController][getCorrelationId] - " +
        s"Error received from DES ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
      case None =>
        val correlationId = UUID.randomUUID().toString
        logger.info("[EopsObligationsController][getCorrelationId] - " +
          s"Validation error: ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
    }
  }

}