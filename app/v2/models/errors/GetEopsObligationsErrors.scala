/*
 * Copyright 2021 HM Revenue & Customs
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

package v2.models.errors

object GetEopsObligationsErrors {

  object MissingFromDateError extends Error("MISSING_FROM_DATE", "The From date parameter is missing")

  object InvalidFromDateError extends Error("FORMAT_FROM_DATE", "The format of the From date is invalid")

  object MissingToDateError extends Error("MISSING_TO_DATE", "The To date parameter is missing")

  object InvalidToDateError extends Error("FORMAT_TO_DATE", "The format of the To date is invalid")

  object RangeTooBigError extends Error("RANGE_DATE_TOO_LONG", "The date range is too long")

}
