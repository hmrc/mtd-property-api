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

import support.UnitSpec
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors.{BadRequestError, ErrorWrapper, InvalidNinoError}

class ValidatorSpec extends UnitSpec {

  val nino: String = "AA123456A"
  val from: String = "2018-01-01"
  val to: String = "2018-12-31"

  class Test {
    object TestValidator extends Validator
  }

  "validate nino" should {
    "return an Invalid Nino error" in new Test {
      TestValidator.validateNino("derp") shouldBe Some(InvalidNinoError)
    }
    "return nothing" in new Test {
      TestValidator.validateNino(nino) shouldBe None
    }
  }

  "the standard 'from' date check" should {
    "return the standard missing date error" when {
      "a date is missing" in new Test {
        TestValidator.fromDateError("") shouldBe Some(MissingStartDateError)
      }
    }
    "return the standard invalid date error" when {
      "a date is not supplied" in new Test {
        TestValidator.fromDateError("derp") shouldBe Some(InvalidStartDateError)
      }
      "the date is not of the correct format" in new Test {
        TestValidator.fromDateError("01-01-2018") shouldBe Some(InvalidStartDateError)
      }
    }
    "return nothing" in new Test {
      TestValidator.fromDateError(from) shouldBe None
    }
  }

  "the standard 'to' date check" should {
    "return the missing date error" when {
      "a date is missing and the missing date error is supplied" in new Test {
        TestValidator.toDateError("") shouldBe Some(MissingEndDateError)

      }
    }
    "return the invalid 'to' date error" when {
      "the data is invalid and the invalid 'to' date error is supplied" in new Test {
        TestValidator.toDateError("derp") shouldBe Some(InvalidEndDateError)
      }
      "the date is not of the correct format" in new Test {
        TestValidator.toDateError("01-01-2018") shouldBe Some(InvalidEndDateError)
      }
    }
    "return nothing" in new Test {
      TestValidator.toDateError(to) shouldBe None
    }
  }

  "the standard invalid date check" should {
    "return the invalid date error" when {
      "'to' date is after 'from' date" in new Test {
        TestValidator.dateRangeError(LocalDate.parse(to), LocalDate.parse(from)) shouldBe Some(InvalidRangeError)
      }
    }
    "return nothing" when {
      "the 'from' date is before the 'to' date" in new Test {
        TestValidator.dateRangeError(LocalDate.parse(from), LocalDate.parse(to)) shouldBe None
      }
      "the 'from' date and the 'to' date are the same" in new Test {
        TestValidator.dateRangeError(LocalDate.parse(from), LocalDate.parse(from)) shouldBe None
      }
    }
  }

  "validation errors" should {
    "return a list of errors" when {
      "more than one error is returned" in new Test {
        TestValidator.validationErrors(Some(MissingEndDateError), Some(InvalidStartDateError), Some(InvalidNinoError)) shouldBe
          Some(ErrorWrapper(BadRequestError, Some(Seq(MissingEndDateError, InvalidStartDateError, InvalidNinoError))))
      }
      "return the correct error" when {
        "a single error is returned" in new Test {
          TestValidator.validationErrors(Some(InvalidEndDateError)) shouldBe Some(ErrorWrapper(InvalidEndDateError, None))
        }
      }
      "return nothing" when {
        "no errors are returned" in new Test {
          TestValidator.validationErrors(None) shouldBe None
        }
        "when an empty list of errors is returned" in new Test {
          TestValidator.validationErrors(None, None, None) shouldBe None
        }
      }
    }
  }

}
