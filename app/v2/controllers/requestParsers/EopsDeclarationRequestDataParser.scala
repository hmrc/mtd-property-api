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

package v2.controllers.requestParsers

import java.time.LocalDate

import javax.inject.Inject
import v2.models.domain.Nino
import v2.controllers.requestParsers.validators.EopsDeclarationInputDataValidator
import v2.models.domain.EopsDeclarationSubmission
import v2.models.errors.{BadRequestError, ErrorWrapper}
import v2.models.inbound.EopsDeclarationRawData
import v2.utils.Logging

class EopsDeclarationRequestDataParser @Inject()(validator: EopsDeclarationInputDataValidator) extends Logging{

  def parseRequest(data: EopsDeclarationRawData)(implicit correlationId: String): Either[ErrorWrapper, EopsDeclarationSubmission] = {

    lazy val eopsDeclarationSubmission =
      EopsDeclarationSubmission(Nino(data.nino), LocalDate.parse(data.start), LocalDate.parse(data.end))

    validator.validate(data) match {
      case Nil =>
        logger.info(
          "[RequestParser][parseRequest] " +
            s"Validation successful for the request with CorrelationId: $correlationId")
        Right(eopsDeclarationSubmission)
      case err :: Nil =>
        logger.warn(
          "[RequestParser][parseRequest] " +
            s"Validation failed with ${err.code} error for the request with CorrelationId: $correlationId")
        Left(ErrorWrapper(correlationId, err, None))
      case errs =>
        logger.warn(
          "[RequestParser][parseRequest] " +
            s"Validation failed with ${errs.map(_.code).mkString(",")} error for the request with CorrelationId: $correlationId")
        Left(ErrorWrapper(correlationId, BadRequestError, Some(errs)))
    }
  }

}
