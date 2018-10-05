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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.controllers.validators.EopsDeclarationValidator
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EopsDeclarationService @Inject()(connector: DesConnector) {

  def submit(nino: String, startDate: String, endDate: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ErrorResponse]] = {

      connector.submitEOPSDeclaration(nino, startDate, endDate).map {
        case Some(SingleError(error)) => Some(ErrorResponse(desErrorToMtdError(error.code), None))
        case Some(MultipleErrors(errors)) => Some(ErrorResponse(BadRequestError, Some(errors.map(_.code).map(desErrorToMtdError))))
        case Some(MultipleBVRErrors(errors)) => Some(ErrorResponse(BVRError, Some(errors.map(_.code).map(desBvrErrorToMtdError))))
        case _ => None
      }
    }

  private val desErrorToMtdError: Map[String, Error] = Map(
    "NOT_FOUND" -> NotFoundError,
    "INVALID_IDTYPE" -> DownstreamError,
    "INVALID_IDVALUE" -> InvalidNinoError,
    "INVALID_ACCOUNTINGPERIODSTARTDATE" -> InvalidStartDateError,
    "INVALID_ACCOUNTINGPERIODENDDATE" -> InvalidEndDateError,
    "CONFLICT" -> ConflictError,
    "EARLY_SUBMISSION" -> EarlySubmissionError,
    "LATE_SUBMISSION" -> LateSubmissionError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> ServiceUnavailableError
  )

  private val desBvrErrorToMtdError: Map[String, Error] = Map(
    "C55317" -> RuleClass4Over16,
    "C55318" -> RuleClass4PensionAge,
    "C55501" -> RuleFhlPrivateUseAdjustment,
    "C55502" -> RuleNonFhlPrivateUseAdjustment,
    "C55008" -> RuleMismatchStartDate,
    "C55013" -> RuleMismatchEndDate,
    "C55014" -> RuleMismatchEndDate
  )
}
