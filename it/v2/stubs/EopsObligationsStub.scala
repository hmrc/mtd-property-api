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

package v2.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status._
import play.api.libs.json.JsValue
import support.WireMockMethods
import v2.fixtures.{DESErrorsFixture, EopsObligationsFixture}

object EopsObligationsStub extends WireMockMethods {

  private val obligations: String=>String = nino => s"/enterprise/obligation-data/nino/$nino/ITSA"

  def successfulEopsObligations(nino: String): StubMapping = {
    when(method = GET, uri = obligations(nino), queryParams = Map("from"-> "2017-04-06", "to" -> "2018-04-05"))
      .thenReturn(status = OK, body = successResponse)
  }

  def unsuccessfulEopsObligations(nino: String): StubMapping = {
    when(method = GET, uri = obligations(nino), queryParams = Map("from"-> "2017-04-06", "to" -> "2018-04-05"))
      .thenReturn(status = INTERNAL_SERVER_ERROR, body = errorResponse)
  }

  lazy val successResponse: JsValue = EopsObligationsFixture.desEOPSJson
  lazy val errorResponse: JsValue = DESErrorsFixture.serverErrorJson
}
