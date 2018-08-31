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

package v2.models

import play.api.libs.json.Json
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class eopsDeclarationSpec extends UnitSpec with JsonErrorValidators{

  val eopsDeclarationRequestJson =
    """
      |{
      |"finalised": true
      |}
    """.stripMargin

  val eopsDeclarationRequest = EopsDeclaration(finalised = true)

  val desEopsDeclarationRequestJson =Json.parse(
    """
      |{
      |"EOPSDeclarationTimestamp": "2018-08-27"
      |}
    """.stripMargin)

  val desEopsDeclarationRequest = DesEopsDeclaration(EOPSDeclarationTimestamp = "2018-08-27")

  "eopsDeclaration reads" should {

    import JsonError._

    "return correct validation errors" when {
      testMandatoryProperty[EopsDeclaration](eopsDeclarationRequestJson)(property = "finalised")

      testPropertyType[EopsDeclaration](eopsDeclarationRequestJson)(
        property = "finalised",
        invalidValue = "6",
        errorPathAndError = "/incomeSourceID" -> BOOLEAN_FORMAT_EXCEPTION
      )
    }

    "return a successfully read uk-property eopsDeclaration model" when {
      "all fields exist" in {
        Json.parse(eopsDeclarationRequestJson).as[EopsDeclaration] shouldBe eopsDeclarationRequest
      }

    }
  }

  "desEopsDeclaration writes" should {
    "render the correct Json" when {

      "all fields exist" in {
        Json.toJson(desEopsDeclarationRequest) shouldBe desEopsDeclarationRequestJson
      }
    }
  }
}
