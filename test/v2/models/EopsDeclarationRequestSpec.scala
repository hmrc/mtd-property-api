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

package v2.models

import play.api.libs.json.Json
import support.UnitSpec
import v2.models.inbound.EopsDeclarationRequest
import v2.models.utils.JsonErrorValidators

class EopsDeclarationRequestSpec extends UnitSpec with JsonErrorValidators{

  val eopsDeclarationRequestJson =
    """
      |{
      |"finalised": true
      |}
    """.stripMargin

  val eopsDeclarationRequest = EopsDeclarationRequest(finalised = true)

  "eopsDeclaration reads" should {

    import JsonError._

    "return correct validation errors" when {
      testMandatoryProperty[EopsDeclarationRequest](eopsDeclarationRequestJson)(property = "finalised")

      testPropertyType[EopsDeclarationRequest](eopsDeclarationRequestJson)(
        property = "finalised",
        invalidValue = "6",
        errorPathAndError = "/incomeSourceID" -> BOOLEAN_FORMAT_EXCEPTION
      )
    }

    "return a successfully read uk-property eopsDeclaration model" when {
      "all fields exist" in {
        Json.parse(eopsDeclarationRequestJson).as[EopsDeclarationRequest] shouldBe eopsDeclarationRequest
      }

    }
  }
}
