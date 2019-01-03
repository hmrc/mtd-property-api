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

object SubmitEopsDeclarationErrors {

  object ConflictError extends MtdError("RULE_ALREADY_SUBMITTED", "You cannot submit a statement for the same accounting period twice")

  object EarlySubmissionError extends MtdError("RULE_EARLY_SUBMISSION", "You cannot submit a statement before the end of your accounting period")

  object LateSubmissionError extends MtdError("RULE_LATE_SUBMISSION", "The period to finalise has passed")

  object RuleClass4Over16 extends MtdError("RULE_CLASS4_OVER_16",
    "Class 4 exemption is not allowed because the individual's age is greater than or equal to 16 years old on the 6th April of the current tax year.")

  object RuleClass4PensionAge extends MtdError("RULE_CLASS4_PENSION_AGE",
    "Class 4 exemption is not allowed because the individual's age is less than their State Pension age on the 6th April of the current tax year.")

  object RuleFhlPrivateUseAdjustment extends MtdError("RULE_FHL_PRIVATE_USE_ADJUSTMENT",
    "For UK Furnished Holiday Lettings, the private use adjustment must not exceed the total allowable expenses.")

  object RuleNonFhlPrivateUseAdjustment extends MtdError("RULE_NON_FHL_PRIVATE_USE_ADJUSTMENT",
    "For UK non-Furnished Holiday Lettings, the private use adjustment must not exceed the total allowable expenses.")

  object RuleMismatchStartDate extends MtdError("RULE_MISMATCH_START_DATE",
    "The period submission start date must match the accounting period start date.")

  object RuleMismatchEndDate extends MtdError("RULE_MISMATCH_END_DATE",
    "The period submission end date must match the accounting period end date.")

  object RuleConsolidatedExpenses extends MtdError("RULE_CONSOLIDATED_EXPENSES",
    "Consolidated expenses are not allowed if the cumulative turnover amount exceeds the threshold.")

}
