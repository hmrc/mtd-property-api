/*
 * Copyright 2021 HM Revenue & Customs
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

package v2.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status._
import play.api.libs.json.JsValue
import support.WireMockMethods
import v2.fixtures.DESErrorsFixture.{conflictErrorJson, _}

object EopsDeclarationStub extends WireMockMethods {

  val url = (nino: String, from: String, to: String) => s"/income-tax/income-sources/nino/$nino/uk-property/$from/$to/declaration"

  def successfulEopsDeclaration(nino: String, from: String, to: String): StubMapping = {
    when(method = POST, uri = url(nino, from, to))
      .thenReturn(status = NO_CONTENT)
  }

  def unSuccessfulEopsDeclaration(nino: String, from: String, to: String,
                                  status: Int, errorType: String): StubMapping = {
    when(method = POST, uri = url(nino, from, to))
      .thenReturn(status = status, body = desStatusToError(errorType))
  }

  private val desStatusToError: Map[String, JsValue] = Map(
    "NOT_FOUND" -> notFoundErrorJson,
    "CONFLICT" -> conflictErrorJson,
    "EARLY_SUBMISSION" -> earlySubmissionErrorJson,
    "SERVER_ERROR" -> serverErrorJson,
    "BVR" -> bvrErrorJson,
    "MULTIPLE_ERROR" -> multipleErrorJson
  )
}
