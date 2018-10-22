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

package v2.controllers.validators

import java.time.LocalDate

import javax.inject.Singleton
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Nino
import v2.models.Declaration
import v2.models.errors.SubmitEopsDeclarationErrors.NotFinalisedDeclaration
import v2.models.errors.{BadRequestError, MtdError, ErrorWrapper}


@Singleton
class EopsDeclarationValidator extends Validator {

  def validateSubmit(nino: String, from: String, to: String, requestBody: JsValue): Either[ErrorWrapper, EopsDeclarationSubmission] = {
    validationErrors(
      validateNino(nino),
      fromDateError(from),
      toDateError(to)) match {
      case None =>
        dateRangeError(LocalDate.parse(from), LocalDate.parse(to)) match {
          case None => validateDeclarationBody(requestBody) match {
            case None => Right(EopsDeclarationSubmission(new Nino(nino), LocalDate.parse(from), LocalDate.parse(to)))
            case Some(bodyParseError) => Left(ErrorWrapper(bodyParseError, None))
          }
          case Some(dateRangeErr) => Left(ErrorWrapper(dateRangeErr, None))
        }
      case Some(paramsErr) => Left(paramsErr)
    }
  }

    def validateDeclarationBody(requestBody: JsValue): Option[MtdError] =
      requestBody.asOpt[Declaration] match {
        case Some(declaration) if declaration.finalised => None
        case Some(declaration) if !declaration.finalised => Some(NotFinalisedDeclaration)
        case _ => Some(BadRequestError)
      }

}

case class EopsDeclarationSubmission(nino: Nino, from: LocalDate, to: LocalDate)