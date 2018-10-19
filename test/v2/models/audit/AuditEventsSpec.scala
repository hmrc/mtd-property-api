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

package v2.models.audit

import java.time.LocalDate

import support.UnitSpec

class AuditEventsSpec extends UnitSpec {

  case class TestDetails(nino: String,
                         from: LocalDate,
                         `X-CorrelationId`: String)

  val nino: String = "AA123456A"
  val from: String = "2018-01-01"

  "audit event" should {
    "return valid AuditEvent" when {
      "proper data is supplied" in {
        val auditEvent = AuditEvent[TestDetails](auditType = "MTD-PROPERTY-TEST",
          transactionName = "mtd-property-audit",
          detail = TestDetails(nino, LocalDate.parse(from), "5b85344c1100008e00c6a181"))

        assert(auditEvent.transactionName == "mtd-property-audit")
        assert(auditEvent.auditType == "MTD-PROPERTY-TEST")
        assert(auditEvent.detail.isInstanceOf[TestDetails])
      }
    }
  }
}
