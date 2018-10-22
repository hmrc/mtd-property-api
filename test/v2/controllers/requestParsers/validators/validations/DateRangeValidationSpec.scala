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

import java.time.LocalDate

import support.UnitSpec
import v2.models.errors.{InvalidRangeError, NinoFormatError}
import v2.models.utils.JsonErrorValidators

class DateRangeValidationSpec extends UnitSpec with JsonErrorValidators {

  "validate" should {
    "return no errors" when {
      "when a from date that is before the to date is supplied " in {

        val fromDate = LocalDate.parse("2018-02-01")
        val toDate = LocalDate.parse("2019-02-01")
        val validationResult = DateRangeValidation.validate(fromDate, toDate)
        validationResult.isEmpty shouldBe true

      }
    }

    "return an error" when {
      "when the from date is after the to date" in {

        val fromDate = LocalDate.parse("2019-02-01")
        val toDate = LocalDate.parse("2018-02-01")
        val validationResult = DateRangeValidation.validate(fromDate, toDate)
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe InvalidRangeError

      }
    }

  }
}
