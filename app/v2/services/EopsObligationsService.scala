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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.services

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.domain.{Obligation, ObligationDetails}
import v2.models.errors.GetEopsObligationsErrors._
import v2.models.errors._
import v2.models.outcomes.DesResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class EopsObligationsService @Inject()(connector: DesConnector) {

  def retrieveEopsObligations(nino: String, from: String, to: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EopsObligationsOutcome] = {

    validateGetEopsObligationsArgs(nino, from, to) match {
      case Right((fromDate, toDate)) => retrieveEopsObligations(nino, fromDate, toDate)
      case Left(errors) => Future.successful(Left(errors))
    }

  }

  private def retrieveEopsObligations(nino: String, from: LocalDate, to: LocalDate)
                                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EopsObligationsOutcome] = {

    connector.getObligations(nino, from, to).map {
      case Left(DesResponse(correlationId, singleError :: Nil)) =>
        Left(ErrorWrapper(Some(correlationId), desErrorToMtdError(singleError.code), None))
      case Left(DesResponse(correlationId, errors)) =>
        Left(ErrorWrapper(Some(correlationId), BadRequestError, Some(errors.map(_.code).map(desErrorToMtdError))))
      case Right(DesResponse(correlationId, obligations)) =>
        val eopsObligations = filterEopsObligations(obligations)
        if (eopsObligations.nonEmpty) {
          Right(DesResponse(correlationId, eopsObligations))
        } else {
          Left(ErrorWrapper(Some(correlationId), NotFoundError, None))
        }
    }

  }

  private[services] def filterEopsObligations(obligations: Seq[ObligationDetails]): Seq[Obligation] = {
    obligations
      .filter(_.incomeSourceType.contains("ITSP"))
      .flatMap(_.obligations.filter(_.periodKey == "EOPS"))
  }

  private def validateDate(date: String,
                           missingDateError: Error,
                           invalidDateError: Error): Option[Error] = date.trim match {
    case "" => Some(missingDateError)
    case _ if Try(LocalDate.parse(date)).isFailure => Some(invalidDateError)
    case _ => None
  }

  val desErrorToMtdError: Map[String, Error] = Map(
    "NOT_FOUND" -> NotFoundError,
    "INVALID_IDTYPE" -> NotFoundError,
    "INVALID_IDNUMBER" -> NinoFormatError,
    "INVALID_DATE_TO" -> InvalidToDateError,
    "INVALID_DATE_FROM" -> InvalidFromDateError,
    "INVALID_DATE_RANGE" -> RangeTooBigError,
    "INTERNAL_SERVER_ERROR" -> DownstreamError,
    "NOT_FOUND_BPKEY" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> DownstreamError,
    "SERVER_ERROR" -> DownstreamError,
    "INVALID_STATUS" -> DownstreamError,
    "INVALID_REGIME" -> DownstreamError
  )

  private def validateDateRange(from: LocalDate,
                                to: LocalDate,
                                maxRangeInDays: Int,
                                invalidRangeError: Error,
                                rangeExceededError: Error): Option[Error] = {

    if (to.isBefore(from) || to == from) {
      Some(invalidRangeError)
    } else if (from.plusDays(maxRangeInDays).isBefore(to)) {
      Some(rangeExceededError)
    } else {
      None
    }
  }

  private def validateGetEopsObligationsArgs(nino: String,
                                             from: String,
                                             to: String): Either[ErrorWrapper, (LocalDate, LocalDate)] = {

    val MAX_DATE_RANGE_IN_DAYS = 366

    val ninoError: Option[NinoFormatError.type] = if (!Nino.isValid(nino)) Some(NinoFormatError) else None

    val fromDateError: Option[Error] = validateDate(from, MissingFromDateError, InvalidFromDateError)

    val toDateError: Option[Error] = validateDate(to, MissingToDateError, InvalidToDateError)

    val invalidRangeError: Option[Error] = (fromDateError, toDateError) match {
      case (None, None) =>
        val fromDate = LocalDate.parse(from)
        val toDate = LocalDate.parse(to)
        validateDateRange(fromDate, toDate, MAX_DATE_RANGE_IN_DAYS, RangeToDateBeforeFromDateError, RangeTooBigError)
      case _ => None
    }

    val validationErrors: Seq[Option[Error]] = Seq(
      ninoError,
      fromDateError,
      toDateError,
      invalidRangeError
    )

    validationErrors.flatten match {
      case List() => Right((LocalDate.parse(from), LocalDate.parse(to)))
      case error :: Nil => Left(ErrorWrapper(None, error, None))
      case errors => Left(ErrorWrapper(None, BadRequestError, Some(errors)))
    }
  }
}
