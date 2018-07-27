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

package v2.services

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.errors.Error
import v2.models.{Obligation, ObligationDetails}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EopsObligationsService @Inject()(connector: DesConnector) {



  def retrieveEopsObligations(nino: String, from: LocalDate, to: LocalDate)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Seq[Error], Seq[Obligation]]] = {
    connector.getObligations(nino, from, to).map {
      case Left(o) => Left(o)
      case Right(o) => Right(filterEopsObligations(o))
    }
  }

  def filterEopsObligations(obligations: Seq[ObligationDetails]): Seq[Obligation] = {
    obligations
      .filter(_.incomeSourceType.contains("ITSP"))
      .flatMap(_.obligations.filter(_.periodKey == "EOPS"))
  }
}
