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

package v2.connectors

import java.time.LocalDate

import play.api.http.HeaderNames
import v2.mocks.{MockAppConfig, MockHttpClient}
import v2.models.errors.DownstreamError
import v2.models.outcomes.ObligationsOutcome

import scala.concurrent.Future

class DesConnectorSpec extends ConnectorSpec {

  val baseUrl = "test-mtdIdBaseUrl"

  private trait Test extends MockHttpClient with MockAppConfig {

    val connector = new DesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "desHeaderCarrier" should {
    "return a header carrier with an authorization header using the DES token specified in config" in new Test {
      connector.desHeaderCarrier.headers.contains(HeaderNames.AUTHORIZATION -> "des-token") shouldBe true
    }

    "return a header carrier with an environment header using the DES environment specified in config" in new Test {
      connector.desHeaderCarrier.headers.contains("Environment" -> "des-environment") shouldBe true
    }
  }

  "getObligations" should {

    val nino = "AA12356A"
    val from = LocalDate.parse("2018-01-01")
    val to = LocalDate.parse("2018-01-01")

    val urlPath = s"/enterprise/obligation-data/nino/$nino/ITSA?from=$from&to=$to"

    "return a collection of obligation details" when {
      "the http client returns a collection of obligations" in new Test {

        MockedHttpClient.get[ObligationsOutcome](baseUrl + urlPath)
          .returns(Future.successful(Right(Seq.empty)))

        val result: ObligationsOutcome = await(connector.getObligations(nino, from, to))
        result shouldBe Right(Seq.empty)
      }
    }

    "return a DownstreamError" when {
      "the http client returns a DownstreamError" in new Test {
        MockedHttpClient.get[ObligationsOutcome](baseUrl + urlPath)
          .returns(Future.successful(Left(Seq(DownstreamError))))

        val result: ObligationsOutcome = await(connector.getObligations(nino, from, to))
        result shouldBe Left(Seq(DownstreamError))
      }
    }
  }
}