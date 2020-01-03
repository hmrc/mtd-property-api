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
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v2.fixtures.EopsObligationsFixture
import v2.models.errors.GetEopsObligationsErrors.{MissingFromDateError, MissingToDateError}
import v2.models.errors.{BadRequestError, DownstreamError, ErrorWrapper}
import v2.stubs.{AuditStub, AuthStub, EopsObligationsStub, MtdIdLookupStub}

class EopsObligationsISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(s"/2.0/ni/$nino/uk-properties/end-of-period-statements/obligations?from=2017-04-06&to=2018-04-05")
    }
  }

  "Calling the EOPS endpoint" should {

    "return a 200 status code and correct body" when {

      "an obligation exists for the NINO within the date range" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          EopsObligationsStub.successfulEopsObligations(nino)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
        response.json shouldBe EopsObligationsFixture.EOPSSuccess
      }
    }

    "return a 500" when {
      "DES returns a server error" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          EopsObligationsStub.unsuccessfulEopsObligations(nino)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.INTERNAL_SERVER_ERROR
        response.json shouldBe Json.toJson(DownstreamError)
      }
    }

    "return a 400" when {
      "a request is made with no query parameters" in new Test {
        override val nino: String = "AA654321A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        override def request(): WSRequest = {
          setupStubs()
          buildRequest(s"/2.0/ni/$nino/uk-properties/end-of-period-statements/obligations")
        }

        val multiDateErrorJson: JsValue = Json.toJson(ErrorWrapper(None, BadRequestError, Some(Seq(MissingFromDateError, MissingToDateError))))

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.BAD_REQUEST
        response.json shouldBe multiDateErrorJson
      }
    }
  }
}
