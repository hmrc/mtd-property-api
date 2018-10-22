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

package v2.controllers.requestParsers

import java.time.LocalDate

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import v2.controllers.requestParsers.validators.EopsDeclarationInputDataValidator
import v2.models.EopsDeclarationSubmission
import v2.models.errors.{BadRequestError, ErrorWrapper}
import v2.models.inbound.EopsDeclarationRequestData

class EopsDeclarationRequestDataParser @Inject()(validator: EopsDeclarationInputDataValidator) {

  def parseRequest(data: EopsDeclarationRequestData): Either[ErrorWrapper, EopsDeclarationSubmission] = {

    lazy val eopsDeclarationSubmission =
      EopsDeclarationSubmission(Nino(data.nino), LocalDate.parse(data.start), LocalDate.parse(data.end))

    validator.validate(data) match {
      case Nil => Right(eopsDeclarationSubmission)
      case err :: Nil => Left(ErrorWrapper(err, None))
      case errs => Left(ErrorWrapper(BadRequestError, Some(errs)))
    }
  }

}
