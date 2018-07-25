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

package v2.mocks.services

import java.time.LocalDate

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v2.models.{Obligation, errors}
import v2.services.EopsObligationsService

import scala.concurrent.{ExecutionContext, Future}

trait MockEopsObligationsService extends MockFactory {

  val mockEopsObligationsService: EopsObligationsService = mock[EopsObligationsService]

  object MockedEopsObligationsService {
    def retrieveEopsObligations(nino: String, from: LocalDate, to: LocalDate): CallHandler[Future[Either[Seq[errors.Error], Seq[Obligation]]]] = {
      (mockEopsObligationsService.retrieveEopsObligations(_: String, _:LocalDate, _:LocalDate)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, from, to, *, *)
    }
  }
}
