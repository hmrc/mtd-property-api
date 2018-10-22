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

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v2.models.EopsDeclarationSubmission
import v2.models.errors.ErrorWrapper
import v2.services.EopsDeclarationService

import scala.concurrent.{ExecutionContext, Future}

trait MockEopsDeclarationService extends MockFactory {

  val mockEopsDeclarationService: EopsDeclarationService = mock[EopsDeclarationService]

  object MockedEopsDeclarationService {
    def submitDeclaration(eopsDeclarationSubmission: EopsDeclarationSubmission): CallHandler[Future[Option[ErrorWrapper]]] = {
      (mockEopsDeclarationService.submit(_: EopsDeclarationSubmission)(_: HeaderCarrier, _: ExecutionContext))
        .expects(eopsDeclarationSubmission, *, *)
    }
  }
}
