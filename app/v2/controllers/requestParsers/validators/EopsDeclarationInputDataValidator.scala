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

package v2.controllers.requestParsers.validators

import java.time.LocalDate

import v2.controllers.requestParsers.validators.validations._
import v2.models.errors._
import v2.models.inbound.{EopsDeclarationRequest, EopsDeclarationRawData}

class EopsDeclarationInputDataValidator extends Validator[EopsDeclarationRawData] {

  private val validationSet = List(levelOneValidations, levelTwoValidations, levelThreeValidations)

  private def levelOneValidations: EopsDeclarationRawData => List[List[Error]] = (data: EopsDeclarationRawData) => {
    List(
      NinoValidation.validate(data.nino),
      NonEmptyValidation.validate(data.start, MissingStartDateError),
      NonEmptyValidation.validate(data.end, MissingEndDateError),
      DateFormatValidation.validate(data.start, InvalidStartDateError),
      DateFormatValidation.validate(data.end, InvalidEndDateError)
    )
  }

  private def levelTwoValidations: EopsDeclarationRawData => List[List[Error]] = (data: EopsDeclarationRawData) => {
    List(
      DateRangeValidation.validate(LocalDate.parse(data.start), LocalDate.parse(data.end)),
      JsonFormatValidation.validate[EopsDeclarationRequest](data.body)
    )
  }

  private def levelThreeValidations: EopsDeclarationRawData => List[List[Error]] = (data: EopsDeclarationRawData) => {
    List(
      EopsDeclarationRequestDataValidation.validate(data.body)
    )
  }

  override def validate(data: EopsDeclarationRawData): List[Error] = {
    run(validationSet, data)
  }

}