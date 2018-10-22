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
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.EopsDeclarationSubmission
import v2.models.errors.SubmitEopsDeclarationErrors._
import v2.models.errors._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EopsDeclarationService @Inject()(connector: DesConnector) {

  def submit(eopsDeclarationSubmission: EopsDeclarationSubmission)
            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ErrorWrapper]] = {

    val logger: Logger = Logger(this.getClass)

    connector.submitEOPSDeclaration(eopsDeclarationSubmission.nino, eopsDeclarationSubmission.start,
      eopsDeclarationSubmission.end).map {
      case Left(SingleError(error)) =>
        Some(ErrorWrapper(desErrorToMtdError(error.code), None))
      case Left(MultipleErrors(errors)) =>
        val mtdErrors = errors.map(error => desErrorToMtdError(error.code))
        if (mtdErrors.contains(DownstreamError)) {
          logger.info("[EopsDeclarationService] [submit] - downstream returned INVALID_IDTYPE. Revert to ISE")
          Some(ErrorWrapper(DownstreamError, None))
        }
        else {
          Some(ErrorWrapper(BadRequestError, Some(mtdErrors)))
        }
      case Left(BVRErrors(errors)) =>
        if(errors.size == 1){
          Some(ErrorWrapper(desBvrErrorToMtdError(errors.head.code), None))
        }else {
          Some(ErrorWrapper(BVRError, Some(errors.map(_.code).map(desBvrErrorToMtdError))))
        }
      case Left(GenericError(error)) => Some(ErrorWrapper(error, None))
      case Right(correlationId) =>
        //audit
        // @TODO Audit implementation
        None
    }
  }

  private val desErrorToMtdError: Map[String, MtdError] = Map(
    "NOT_FOUND" -> NotFoundError,
    "INVALID_IDTYPE" -> DownstreamError,
    "INVALID_IDVALUE" -> NinoFormatError,
    "INVALID_ACCOUNTINGPERIODSTARTDATE" -> InvalidStartDateError,
    "INVALID_ACCOUNTINGPERIODENDDATE" -> InvalidEndDateError,
    "CONFLICT" -> ConflictError,
    "EARLY_SUBMISSION" -> EarlySubmissionError,
    "LATE_SUBMISSION" -> LateSubmissionError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> ServiceUnavailableError
  )

  private val desBvrErrorToMtdError: Map[String, MtdError] = Map(
    "C55317" -> RuleClass4Over16,
    "C55318" -> RuleClass4PensionAge,
    "C55501" -> RuleFhlPrivateUseAdjustment,
    "C55502" -> RuleNonFhlPrivateUseAdjustment,
    "C55008" -> RuleMismatchStartDate,
    "C55013" -> RuleMismatchEndDate,
    "C55014" -> RuleMismatchEndDate,
    "C55503" -> RuleConsolidatedExpenses
  )
}
