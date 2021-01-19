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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import v2.models.errors.GetEopsObligationsErrors._
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.services.{EnrolmentsAuthService, EopsObligationsService, MtdIdLookupService}
import v2.utils.IdGenerator

import scala.concurrent.ExecutionContext

@Singleton
class EopsObligationsController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          val cc: ControllerComponents,
                                          val idGenerator: IdGenerator,
                                          service: EopsObligationsService)(implicit ec: ExecutionContext) extends AuthorisedController(cc) {

  val logger: Logger = Logger(this.getClass)
  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "EopsObligationsController", endpointName = "Get EOPS Obligations")

  def getEopsObligations(nino: String, from: String, to: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val correlationId: String = idGenerator.generateCorrelationId
      logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
          s"with CorrelationId: $correlationId")

      service.retrieveEopsObligations(nino, from, to).map {
        case Left(e) =>
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Error response received with CorrelationId: ${e.correlationId}")
          processError(e).withHeaders("X-CorrelationId" -> e.correlationId)
        case Right(DesResponse(resultCorrelationId, success)) =>
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: $resultCorrelationId")
          Ok(Json.obj("obligations" -> Json.toJson(success))).withHeaders("X-CorrelationId" -> resultCorrelationId)
      }
    }

  private def processError(errorResponse: ErrorWrapper) = {
    (errorResponse.error: @unchecked) match {
      case MissingFromDateError | MissingToDateError
           | InvalidFromDateError | InvalidToDateError
           | RangeToDateBeforeFromDateError | RangeEndDateBeforeStartDateError | RangeTooBigError
           | BadRequestError | NinoFormatError =>
        BadRequest(Json.toJson(errorResponse))
      case NotFoundError => NotFound(Json.toJson(errorResponse))
      case DownstreamError => InternalServerError(Json.toJson(errorResponse))
    }
  }

}
