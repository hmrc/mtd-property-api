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

package v2.mocks.validators

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.JsValue
import v2.controllers.validators.{EopsDeclarationSubmission, EopsDeclarationValidator}
import v2.models.errors.ErrorResponse

trait MockEopsDeclarationValidator extends MockFactory {

  val mockEopsDeclarationValidator: EopsDeclarationValidator = mock[EopsDeclarationValidator]

  object MockEopsDeclarationValidator {
    def validateSubmit(nino: String, from: String, to: String,
                       requestBody: JsValue): CallHandler[Either[ErrorResponse, EopsDeclarationSubmission]] = {
      (mockEopsDeclarationValidator.validateSubmit(_: String, _: String, _: String, _: JsValue))
        .expects(nino, from, to, requestBody)
    }
  }


}
