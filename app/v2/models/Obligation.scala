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

package v2.models

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class Obligation(startDate: LocalDate,
                      endDate: LocalDate,
                      dueDate: LocalDate,
                      status: ObligationStatus,
                      processedDate: Option[LocalDate],
                      periodKey: String) {

  if (status == FulfilledObligation && processedDate.isEmpty) {
    throw new Exception("Cannot create a fulfilled obligation without a processed date")
  }

  if (status == OpenObligation && processedDate.nonEmpty) {
    throw new Exception("Cannot create an open obligation with a processed date")
  }
}

object Obligation {
  implicit val reads: Reads[Obligation] = (
    (__ \ "inboundCorrespondenceFromDate").read[LocalDate] and
    (__ \ "inboundCorrespondenceToDate").read[LocalDate] and
    (__ \ "inboundCorrespondenceDueDate").read[LocalDate] and
    (__ \ "status").read[ObligationStatus] and
    (__ \ "inboundCorrespondenceDateReceived").readNullable[LocalDate] and
    (__ \ "periodKey").read[String]
  ) (Obligation.apply _)
}