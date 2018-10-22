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

package v2.controllers.requestParsers.validators.validations

import support.UnitSpec
import v2.models.errors.InvalidStartDateError
import v2.models.utils.JsonErrorValidators

class DateFormatValidationSpec extends UnitSpec with JsonErrorValidators {

  "validate" should {
    "return no errors" when {

      "when a date with a valid format is supplied" in {

        val validDate = "2018-01-01"
        val error = InvalidStartDateError

        val validationResult = DateFormatValidation.validate(validDate, error)
        validationResult.isEmpty shouldBe true

      }

    }

    "return the specific error supplied " when {

      "when the format of the date is not correct" in {

        val invalidDate = "123ABC2018-01-01"
        val specificError = InvalidStartDateError

        val validationResult = DateFormatValidation.validate(invalidDate, specificError)
        validationResult.isEmpty shouldBe false
        validationResult.size shouldBe 1
        validationResult.head shouldBe specificError

      }

    }

  }
}
