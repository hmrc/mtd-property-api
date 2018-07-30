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

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import v2.config.AppConfig
import v2.models.outcomes.ObligationsOutcome

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(http: HttpClient,
                             appConfig: AppConfig) {

  private[connectors] def desHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier = hc.withExtraHeaders(
    HeaderNames.AUTHORIZATION -> appConfig.desToken,
    "Environment" -> appConfig.desEnv
  )

  def getObligations(nino: String, from: LocalDate, to: LocalDate)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ObligationsOutcome] = {
    import v2.connectors.httpparsers.ObligationsHttpParser.obligationsHttpReads

    val urlPath = s"/enterprise/obligation-data/nino/$nino/ITSA?from=$from&to=$to"

    http.GET[ObligationsOutcome](appConfig.desBaseUrl + urlPath)(implicitly, desHeaderCarrier, implicitly)
  }
}