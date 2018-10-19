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

package v2.mocks.connectors

import java.time.LocalDate

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.outcomes.{EopsDeclarationOutcome, ObligationsOutcome}

import scala.concurrent.{ExecutionContext, Future}

trait MockDesConnector extends MockFactory {

  val mockDesConnector: DesConnector = mock[DesConnector]

  object MockedDesConnector {
    def getObligations(nino: String, from: LocalDate, to: LocalDate): CallHandler[Future[ObligationsOutcome]] = {
      (mockDesConnector.getObligations(_: String, _: LocalDate, _: LocalDate)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, from, to, *, *)
    }

    def submitEOPSDeclaration(nino: Nino, from: LocalDate, to: LocalDate): CallHandler[Future[EopsDeclarationOutcome]] = {
      (mockDesConnector.submitEOPSDeclaration(_: Nino, _: LocalDate, _: LocalDate)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, from, to, *, *)
    }
  }

}
