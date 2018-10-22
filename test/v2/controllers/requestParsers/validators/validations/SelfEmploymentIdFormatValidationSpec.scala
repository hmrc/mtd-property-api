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
import v2.models.errors.SelfEmploymentIdError
import v2.models.utils.JsonErrorValidators

class SelfEmploymentIdFormatValidationSpec extends UnitSpec with JsonErrorValidators {

  "validate" should {
    "return no errors" when {

      "when a self employment id with a valid format is supplied" in {
        val validSelfEmploymentId = "X1AAAAAAAAAAAA5"

        val validationResult = SelfEmploymentIdFormatValidation.validate(validSelfEmploymentId)
        validationResult.isEmpty shouldBe true
      }

    }

    "return an error " when {

      "when the employment id is too long" in {

        val tooLongSelfEmploymentId = "AAAAAAAAAAAAAAAAAAAAAA"

        val validationResult = SelfEmploymentIdFormatValidation.validate(tooLongSelfEmploymentId)
        validationResult.isEmpty shouldBe false
        validationResult.head shouldBe SelfEmploymentIdError

      }

      "when the employment id is empty" in {

        val emptySelfEmploymentId = ""

        val validationResult = SelfEmploymentIdFormatValidation.validate(emptySelfEmploymentId)
        validationResult.isEmpty shouldBe false
        validationResult.head shouldBe SelfEmploymentIdError

      }

      "when the employment id contains invalid characters" in {

        val specialCharactersSelfEmploymentId = "X1AAAAAAAA^&AA5"

        val validationResult = SelfEmploymentIdFormatValidation.validate(specialCharactersSelfEmploymentId)
        validationResult.isEmpty shouldBe false
        validationResult.head shouldBe SelfEmploymentIdError

      }

    }

  }
}
