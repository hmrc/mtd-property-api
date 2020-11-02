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

package v2.controllers.requestParsers

import java.time.LocalDate

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v2.mocks.validators.MockEopsDeclarationInputDataValidator
import v2.models.domain.EopsDeclarationSubmission
import v2.models.errors.{BadRequestError, ErrorWrapper, MissingStartDateError, NinoFormatError}
import v2.models.inbound.EopsDeclarationRawData

class EopsDeclarationRequestDataParserSpec extends UnitSpec {

  implicit val correlationId: String = "x1234id"

  trait Test extends MockEopsDeclarationInputDataValidator {
    lazy val parser = new EopsDeclarationRequestDataParser(mockValidator)

    // WLOG - validation is mocked
    val rawData: EopsDeclarationRawData = EopsDeclarationRawData("AA112233A", "2018-01-01", "2019-01-01", AnyContentAsJson(Json.obj()))
    val submission: EopsDeclarationSubmission = EopsDeclarationSubmission(Nino("AA112233A"), LocalDate.parse("2018-01-01"), LocalDate.parse("2019-01-01"))
  }

  "EopsDeclarationRequestDataParser" when {
    "raw data is valid" must {
      "return the parsed submission" in new Test {
        MockedEopsDeclarationInputDataValidator.validate(rawData).returns(Nil)

        parser.parseRequest(rawData) shouldBe Right(submission)
      }
    }

    "raw data has a single error" must {
      "return the error" in new Test {
        MockedEopsDeclarationInputDataValidator.validate(rawData).returns(List(NinoFormatError))

        parser.parseRequest(rawData) shouldBe Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }
    }

    "raw data has multiple errors" must {
      "return the multple errors with BadRequestError as main error" in new Test {
        MockedEopsDeclarationInputDataValidator.validate(rawData).returns(List(NinoFormatError, MissingStartDateError))
        parser.parseRequest(rawData) shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(List(NinoFormatError, MissingStartDateError))))
      }
    }
  }
}
