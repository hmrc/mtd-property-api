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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.errors.GetEopsObligationsErrors._
import v2.models.errors.{BadRequestError, Error, ErrorResponse, InvalidNinoError, NotFoundError}
import v2.models.outcomes.EopsObligationsOutcome
import v2.models.{Obligation, ObligationDetails}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class EopsObligationsService @Inject()(connector: DesConnector) {

  def retrieveEopsObligations(nino: String, from: String, to: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EopsObligationsOutcome] = {

    validateGetEopsObligationsInput(nino, from, to) match {
      case Right((fromDate, toDate)) => retrieveEopsObligations(nino, fromDate, toDate)
      case Left(errors) => Future.successful(Left(errors))
    }

  }

  private[services] def retrieveEopsObligations(nino: String, from: LocalDate, to: LocalDate)
                                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EopsObligationsOutcome] = {

    connector.getObligations(nino, from, to).map {
      case Left(errors) => Left(ErrorResponse(Error("CODE", "message"), Some(errors)))
      case Right(obligations) =>
        val eopsObligations = filterEopsObligations(obligations)
        if (eopsObligations.size > 0) {
          Right(eopsObligations)
        } else {
          Left(ErrorResponse(NotFoundError, None))
        }
    }

  }

  private[services] def filterEopsObligations(obligations: Seq[ObligationDetails]): Seq[Obligation] = {
    obligations
      .filter(_.incomeSourceType.contains("ITSP"))
      .flatMap(_.obligations.filter(_.periodKey == "EOPS"))
  }

  def validateDate(date: String): (Boolean, Boolean) = date.trim match {
    case "" => (false, true)
    case _ => (true, Try(LocalDate.parse(date)).isSuccess)
  }

  private[services] def validateGetEopsObligationsInput(nino: String,
                                                        from: String,
                                                        to: String): Either[ErrorResponse, (LocalDate, LocalDate)] = {

    val ninoError = if (!Nino.isValid(nino)) Some(InvalidNinoError) else None

    val fromDateError = validateDate(from) match {
      case (false, _) => Some(MissingFromDateError)
      case (_, false) => Some(InvalidFromDateError)
      case _ => None
    }

    val toDateError = validateDate(to) match {
      case (false, _) => Some(MissingToDateError)
      case (_, false) => Some(InvalidToDateError)
      case _ => None
    }

    val invalidRangeError = (fromDateError, toDateError) match {
      case (None, None) =>
        val fromDate = LocalDate.parse(from)
        val toDate = LocalDate.parse(to)
        if (toDate.isBefore(fromDate)) {
          Some(InvalidRangeError)
        } else if (fromDate.plusDays(366).isBefore(toDate)) {
          Some(RangeTooBigError)
        } else {
          None
        }
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
      case error :: Nil => Left(ErrorResponse(error, None))
      case errors => Left(ErrorResponse(BadRequestError, Some(errors)))
    }
  }
}
