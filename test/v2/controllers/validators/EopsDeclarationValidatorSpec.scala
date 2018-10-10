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

import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v2.models.errors.{BadRequestError, ErrorResponse, InvalidNinoError}
import v2.models.errors.SubmitEopsDeclarationErrors.{InvalidStartDateError, NotFinalisedDeclaration}

class EopsDeclarationValidatorSpec extends UnitSpec {

  val nino: String = "AA123456A"
  val from: String = "2018-01-01"
  val to: String = "2018-12-31"
  val requestJson: JsValue = Json.parse(
    """
      |{
      |"finalised": true
      |}
    """.stripMargin)

  val invalidRequestJson: JsValue = Json.parse(
    """
      |{
      |"finalised": false
      |}
    """.stripMargin)

  val eopsDeclarationSubmission = EopsDeclarationSubmission(Nino(nino), LocalDate.parse(from), LocalDate.parse(to))

  class Test {
    object TestValidator extends EopsDeclarationValidator
  }

  "validateSubmit" should {
    "return a valid EopsDeclarationSubmission" when {
      "a valid NINO, from and to date, with declaration as true is passed" in new Test {
        val result = TestValidator.validateSubmit(nino, from, to, requestJson)
        result shouldBe Right(eopsDeclarationSubmission)
      }
    }

    "return NotFinalisedDeclaration error" when {
      "a valid NINO, from and to date, with declaration as false is passed" in new Test {
        val result = TestValidator.validateSubmit(nino, from, to, invalidRequestJson)
        result shouldBe Left(ErrorResponse(NotFinalisedDeclaration, None))
      }
    }

    "return a single error" when {
      "an invalid NINO with an invalid request body passed" in new Test {
        val result = TestValidator.validateSubmit("boop", from, to, Json.parse("""{}""".stripMargin))
        result shouldBe Left(ErrorResponse(InvalidNinoError, None))
      }
      "an invalid date with an invalid request body is passed" in new Test {
        val result = TestValidator.validateSubmit(nino, "notadate", to, Json.parse("""{}""".stripMargin))
        result shouldBe Left(ErrorResponse(InvalidStartDateError, None))
      }
      "an invalid NINO is supplied with an invalid date range" in new Test {
        val result = TestValidator.validateSubmit("boop", to, from, requestJson)
        result shouldBe Left(ErrorResponse(InvalidNinoError, None))
      }
    }
  }
}
