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
import v2.models.errors.ErrorResponse

@Singleton
class EopsDeclarationValidator extends Validator {

  def validateSubmit(nino: String, from: String, to: String, requestBody: JsValue): Either[ErrorResponse, EopsDeclarationSubmission] =
    validationErrors(validateNino(nino), fromDateError(from), toDateError(to)) match {
      case None => Right(EopsDeclarationSubmission(
        new Nino(nino),
        LocalDate.parse(from),
        LocalDate.parse(to))
      )
      case Some(error) => Left(error)

    }
}


case class EopsDeclarationSubmission(nino: Nino, from: LocalDate, to: LocalDate)