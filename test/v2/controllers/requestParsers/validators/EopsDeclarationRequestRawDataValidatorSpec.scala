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

package v2.controllers.requestParsers.validators

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v2.models.errors._
import v2.models.inbound.EopsDeclarationRawData

class EopsDeclarationRequestRawDataValidatorSpec extends UnitSpec {

  val validNino = "AA123456A"
  val validFromDate = "2018-01-01"
  val validToDate = "2018-12-31"
  val validJsonBody = AnyContentAsJson(Json.obj("finalised" -> true))

  private trait Test {
    val validator = new EopsDeclarationInputDataValidator()
  }

  "running a validation" should {

    "return no errors" when {
      "when the uri is valid and the JSON payload is Valid" in new Test {
        val inputData = EopsDeclarationRawData(validNino, validFromDate, validToDate, validJsonBody)

        val result = validator.validate(inputData)
        result.isEmpty shouldBe true
      }
    }

    "return a single error" when {

      "when an invalid NINO is supplied" in new Test {
        val invalidNino = "ABCDEFGHIJKLM"
        val inputData = EopsDeclarationRawData(invalidNino, validFromDate, validToDate, validJsonBody)

        val result = validator.validate(inputData)
        result.size shouldBe 1
        result.head shouldBe NinoFormatError
      }

      "when a from date with an invalid format is supplied " in new Test {
        val invalidFormatFromDate = "ABCDEFGHIJKLM"
        val inputData = EopsDeclarationRawData(validNino, invalidFormatFromDate, validToDate, validJsonBody)

        val result = validator.validate(inputData)
        result.size shouldBe 1
        result.head shouldBe InvalidStartDateError
      }

      "when a to date with an invalid format is supplied " in new Test {
        val invalidFormatToDate = "ABCDEFGHIJKLM"
        val inputData = EopsDeclarationRawData(validNino, validFromDate, invalidFormatToDate, validJsonBody)

        val result = validator.validate(inputData)
        result.size shouldBe 1
        result.head shouldBe InvalidEndDateError
      }

      "when the declaration has not been finalised" in new Test {
        val invalidJson = AnyContentAsJson(Json.obj("finalised" -> false))
        val inputData = EopsDeclarationRawData(validNino, validFromDate, validToDate, invalidJson)

        val result = validator.validate(inputData)
        result.isEmpty shouldBe false
        result.head shouldBe NotFinalisedDeclaration
      }

    }

    "return multiple errors" when {
      "when the from date is missing " in new Test {
        val emptyFromDate = ""
        val inputData = EopsDeclarationRawData(validNino, emptyFromDate, validToDate, validJsonBody)

        val result = validator.validate(inputData)
        result.size shouldBe 2
        result.contains(MissingStartDateError) shouldBe true
        result.contains(InvalidStartDateError) shouldBe true
      }

      "when the to date is missing " in new Test {
        val emptyToDate = ""
        val inputData = EopsDeclarationRawData(validNino, validFromDate, emptyToDate, validJsonBody)

        val result = validator.validate(inputData)
        result.size shouldBe 2
        result.contains(MissingEndDateError) shouldBe true
        result.contains(InvalidEndDateError) shouldBe true
      }
    }

    "should run level 2 validations" when {
      "when all level 1 validations pass" in new Test {
        val invalidValuesInJson = AnyContentAsJson(Json.obj("fin" -> 123))
        val inputData = EopsDeclarationRawData(validNino, validFromDate, validToDate, invalidValuesInJson)

        val result = validator.validate(inputData)
        result.isEmpty shouldBe false
        result.head shouldBe BadRequestError
      }
    }

    "short circuit and not run level 2 validations" when {
      "when a level 1 error exists" in new Test {
        // Level 1 error
        val invalidFormatToDate = "ABCDEFGHIJKLM"
        // Level 2 error
        val invalidJson = AnyContentAsJson(Json.obj("finalised" -> false))

        val inputData = EopsDeclarationRawData(validNino, validFromDate, invalidFormatToDate, invalidJson)

        val result = validator.validate(inputData)
        result.size shouldBe 1
        result.head shouldBe InvalidEndDateError
      }
    }

    "short circuit and not run level 3 validations" when {
      "when a level 2 error exists" in new Test {

        // Level 1 error should be all valid
        // Level 2 error exists
        val invalidJson = AnyContentAsJson(Json.obj("someMadeUpField" -> false))

        val inputData = EopsDeclarationRawData(validNino, validFromDate, validToDate, invalidJson)

        val result = validator.validate(inputData)
        result.size shouldBe 1

        // Should only be a BadRequestError and the NotFinalisedDeclaration should not be generated
        result.head shouldBe BadRequestError


      }
    }

  }

}
