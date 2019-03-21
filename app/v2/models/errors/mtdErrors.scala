/*
 * Copyright 2019 HM Revenue & Customs
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

// Nino Errors
object NinoFormatError extends Error("FORMAT_NINO", "The NINO format is invalid")

// Date Errors
object MissingStartDateError extends Error("MISSING_START_DATE", "Missing account period start date")
object MissingEndDateError extends Error("MISSING_END_DATE", "Missing account period end date")
object InvalidStartDateError extends Error("FORMAT_START_DATE", "Invalid account period start date")
object InvalidEndDateError extends Error("FORMAT_END_DATE", "Invalid account period end date")
object RangeToDateBeforeFromDateError extends Error("RANGE_TO_DATE_BEFORE_FROM_DATE", "The To date must be after the From date")
object RangeEndDateBeforeStartDateError extends Error("RANGE_END_DATE_BEFORE_START_DATE", "The End date must be after the Start date")


// TODO Check where this should live - EOPSDeclaration specific
object NotFinalisedDeclaration extends
  Error("RULE_NOT_FINALISED", "The statement cannot be accepted without a declaration that it is finalised.")

object SelfEmploymentIdError extends Error("FORMAT_SELF_EMPLOYMENT_ID", "The format of the provided self-employment ID is invalid.")


//Standard Errors

object DownstreamError extends Error("INTERNAL_SERVER_ERROR", "An internal server error occurred")

object NotFoundError extends Error("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found")

object BadRequestError extends Error("INVALID_REQUEST", "Invalid request")

object BVRError extends Error("BUSINESS_ERROR", "Business validation error")

object ServiceUnavailableError extends Error("SERVICE_UNAVAILABLE", "Internal server error")


//Authorisation Errors
object UnauthorisedError extends Error("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised.")
