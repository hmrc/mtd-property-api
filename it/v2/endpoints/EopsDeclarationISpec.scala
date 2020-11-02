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

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._
import v2.stubs._

class EopsDeclarationISpec extends IntegrationBaseSpec with Status {

  private trait Test {

    val nino: String = "AA123456A"
    val from: String = "2017-04-06"
    val to: String = "2018-04-05"

    val requestJson: JsValue = Json.parse(
      """
        |{
        |"finalised": true
        |}
      """.stripMargin)

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(s"/2.0/ni/$nino/uk-properties/end-of-period-statements/from/${LocalDate.parse(from)}/to/${LocalDate.parse(to)}")
    }
  }

  "Calling the EOPS declaration endpoint" should {

    "return a 204 status code" when {

      "submitted with valid data" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          EopsDeclarationStub.successfulEopsDeclaration(nino, from, to)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe NO_CONTENT
      }
    }

    "return a 204 for an agent" when {

      "agent submitted with valid data" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorisedAgent()
          MtdIdLookupStub.ninoFound(nino)
          EopsDeclarationStub.successfulEopsDeclaration(nino, from, to)
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe NO_CONTENT
      }
    }

    "return single error 403 (Forbidden)" when {
      "submitted with valid data" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          EopsDeclarationStub.unSuccessfulEopsDeclaration(nino, from, to, CONFLICT, "CONFLICT")
        }

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe FORBIDDEN
        response.json shouldBe Json.toJson(ErrorWrapper("", ConflictError, None))
      }
    }

    "return bvr multiple errors 403" when {
      "submitted with invalid data" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          EopsDeclarationStub.unSuccessfulEopsDeclaration(nino, from, to, FORBIDDEN, "BVR")
        }

        val expected: ErrorWrapper = ErrorWrapper("", BVRError, Some(Seq(RuleClass4Over16, RuleClass4PensionAge)))

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe FORBIDDEN
        response.json shouldBe Json.toJson(expected)
      }
    }

    "return non-bvr multiple errors 400" when {
      "submitted with invalid data" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          EopsDeclarationStub.unSuccessfulEopsDeclaration(nino, from, to, BAD_REQUEST, "MULTIPLE_ERROR")
        }

        val expected: ErrorWrapper = ErrorWrapper("", BadRequestError, Some(Seq(InvalidStartDateError, InvalidEndDateError)))

        val response: WSResponse = await(request().post(requestJson))
        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson(expected)
      }
    }
  }
}
