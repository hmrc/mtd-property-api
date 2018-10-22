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

package v2.controllers.validators

import java.time.LocalDate

import uk.gov.hmrc.domain.Nino
import v2.models.errors.SubmitEopsDeclarationErrors.{InvalidRangeError, _}
import v2.models.errors.{BadRequestError, MtdError, ErrorWrapper, InvalidNinoError}

import scala.util.{Failure, Try}

trait Validator {

  def validateNino(nino: String): Option[InvalidNinoError.type] = if (!Nino.isValid(nino)) Some(InvalidNinoError) else None

  def fromDateError(from: String): Option[MtdError] = validateDate(from, MissingStartDateError, InvalidStartDateError)

  def toDateError(to: String): Option[MtdError] = validateDate(to, MissingEndDateError, InvalidEndDateError)

  def dateRangeError(from: LocalDate, to: LocalDate): Option[MtdError] = if (from.isAfter(to)) Some(InvalidRangeError) else None

  private def validateDate(date: String,
                           missingDateError: MtdError,
                           invalidDateError: MtdError): Option[MtdError] = {
    val dateRegex = "([0-9]{4}\\-[0-9]{2}\\-[0-9]{2})"

    date.trim match {
      case "" => Some(missingDateError)
      case _ if Try(LocalDate.parse(date)).isFailure => Some(invalidDateError)
      case d => if (d.matches(dateRegex)) None else Some(invalidDateError)
    }
  }

  def validationErrors(errors: Option[MtdError]*): Option[ErrorWrapper] = {
    errors.flatten match {
      case Seq() => None
      case err +: Nil => Some(ErrorWrapper(err, None))
      case errs => Some(ErrorWrapper(BadRequestError, Some(errs)))
    }
  }

}
