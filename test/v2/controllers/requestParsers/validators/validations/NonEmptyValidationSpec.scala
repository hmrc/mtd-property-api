/*
 * Copyright 2020 HM Revenue & Customs
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

package v2.controllers.requestParsers.validators.validations

import support.UnitSpec
import v2.models.errors.{MissingStartDateError, NinoFormatError}
import v2.models.utils.JsonErrorValidators

class NonEmptyValidationSpec extends UnitSpec {

  "validate" should {
    "return no errors" when {
      "when a non empty string is supplied" in {

        val nonEmptyString = "SOMETHING SOMETHING"
        val specificError = MissingStartDateError

        val validationResult = NonEmptyValidation.validate(nonEmptyString, specificError)
        validationResult.isEmpty shouldBe true

      }
    }

    "return the specific error supplied " when {
      "when an empty string is supplied" in {

        val emptyString = ""
        val specificError = MissingStartDateError

        val validationResult = NonEmptyValidation.validate(emptyString, specificError)
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe specificError

      }
    }

  }
}
